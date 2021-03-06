package org.trln.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DocumentLoader {

    private Logger logger = LogManager.getLogger(DocumentLoader.class);

        private BlockingQueue<Optional<Path>> pathQueue;

        private Config config;

        private DataSource dataSource;

        private AtomicInteger counter = new AtomicInteger(0);

        private Output output;

        public DocumentLoader(BlockingQueue<Optional<Path>> pathQueue, Config config) {
            this.pathQueue = pathQueue;
            this.config = config;
            this.dataSource = createDataSource(config);
        }

        private  DataSource createDataSource(Config config) {
            PGSimpleDataSource dataSource = new PGSimpleDataSource();
            dataSource.setServerNames(new String[]{config.getHost()});
            dataSource.setDatabaseName(config.getName());
            dataSource.setUser(config.getUser());
            dataSource.setPassword(config.getPassword());
	    dataSource.setReadOnly(true);
	    dataSource.setDefaultRowFetchSize(250);
            return dataSource;
        }

    private class Output implements AutoCloseable {
            private Path path;
            private Writer writer;
            Output() {
                try {
                    path = Files.createTempFile("argot-", ".json");
                    DocumentLoader.this.logger.debug("Creating new tempfile at {}", path);
                    writer = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8);
                } catch( IOException iox ) {
                    DocumentLoader.this.logger.fatal("Unable to create temp file", iox);
		    throw new RuntimeException(iox);
                }
            }

            public Path getPath() {
                return path;
            }

            public void addDocument(Document d) {
                try {
                    writer.write(d.getContent());
                } catch( IOException iox ) {
                    logger.warn("Unable to write document [{}] to file", d.getId(), iox);
                }
            }

            @Override
            public void close() throws Exception {
                if ( writer != null ) {
                    writer.close();
                }
            }
        }

        private Connection getConnection() throws SQLException {
            Connection conn = dataSource.getConnection();
	    conn.setAutoCommit(false);
	    return conn;
        }

        private void close() {
            if ( output == null ) {
                return;
            }
            try {
                output.close();
		logger.info("Adding {} to file queue for ingest", output.path);
                pathQueue.put(Optional.of(output.getPath()));
            } catch( Exception iox ) {
                logger.warn("Unable to close output file {}", output.path, iox);
            } finally {
                output = null;
            }
        }

        void processDocument(Document d) {
            if ( output == null ) {
                output = new Output();
            }
            output.addDocument(d);

            int position = counter.incrementAndGet();
            if ( position % config.getChunkSize() == 0 ) {
		logger.info("Completed chunk.  Closing file");
                close();
            }
        }

        public void run() {
	    logger.info("run() were called");
	    int count = 0;
            try (Connection conn = getConnection()) {
                try (Statement stmt = conn.createStatement()) {
		    stmt.setFetchSize(250);
		    logger.info("Executing primary query {}", config.getQuery());
                    try (ResultSet rs = stmt.executeQuery(config.getQuery()) ) {
			logger.info("Query executed.  Processing");
                        while (rs.next()) {
			    count++;
                            Document doc = new Document();
                            doc.setId(rs.getString("id"));
                            doc.setOwner(rs.getString("owner"));
                            doc.setContent(rs.getString("content"));
			    if ( count % 5000 == 0 ) {
				    logger.info("Total documents: {}", count);
			    }
                            processDocument(doc);
                        }
                    }
		    logger.info("Processed {} documents in total", count);
                }
            } catch (SQLException sqx) {
                logger.fatal("Error reading from database", sqx);
                System.exit(1);
            } finally {
		close();
	    }
        }
}
