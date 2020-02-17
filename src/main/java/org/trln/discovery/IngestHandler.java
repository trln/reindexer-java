package org.trln.discovery;

import com.google.common.io.CharStreams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class IngestHandler implements Runnable {

    private final static Logger logger = LogManager.getLogger(IngestHandler.class);

    private final String solrUrl;

    private final String commandFormat = "argot ingest -s %s";

    private final BlockingQueue<Optional<Path>> queue;

    private static AtomicInteger processCount = new AtomicInteger(0);

    public IngestHandler(BlockingQueue<Optional<Path>> queue, String solrUrl) {
        this.queue = queue;
        this.solrUrl = solrUrl;
    }

    private String readInputStream(InputStream stream) {
        String text;
        try(InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8) ) {
            text = CharStreams.toString(reader);
        } catch( IOException iox ) {
            logger.warn("unable to read stream from process");
            return "";
        }
        return text;
    }

    private String runCommand(Path path) {
        String command = String.format(commandFormat, path.toAbsolutePath().toString());
        try {
            Process p = Runtime.getRuntime().exec(command.split("\\s+"));
            int exitValue = p.waitFor();
            if ( exitValue != 0 ) {
                logger.warn("ansible ingest process failed {}", exitValue);
            }
            return String.format("[stdout] %s [stderr] %s",
                    readInputStream(p.getInputStream()),
                    readInputStream(p.getErrorStream())
            );
        } catch( Exception e ) {
            logger.error("Error running ansible ingest process", e);
        }
        return "<no output available>";
    }

    @Override
    public void run() {
        while( !Thread.currentThread().isInterrupted() ) {
            try {
                Optional<Path> p = queue.take();
                if ( p.isEmpty() ) {
                    logger.info("Got shutdown signal");
                    break;
                }
                int count = processCount.incrementAndGet();
                logger.info("[{}] Received {}", count, p);
                Thread.sleep(2600);
                try {
                    Files.delete(p.get());
                } catch(IOException iox) {
                    iox.printStackTrace(System.err);
                }

            } catch (InterruptedException ix) {
                logger.info("this is where I got shut down");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
