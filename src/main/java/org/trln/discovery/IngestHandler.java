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

    private class ProcessResult {
	    int exitStatus;
	    String stdout;
	    String stderr;

	    ProcessResult(int exitStatus, String stdout, String stderr) {
		    this.exitStatus = exitStatus;
		    this.stdout = stdout;
		    this.stderr = stderr;
	    }

	    public String toString() {
		    return String.format("[%d] [stdout] %s, [stderr] %s", exitStatus, stdout, stderr);
	    }
    }

    private ProcessResult runCommand(Path path) {
	    String errorMessage;
        try {
	    ProcessBuilder pb = new ProcessBuilder("argot", "ingest", "-s", solrUrl, path.toAbsolutePath().toString());
	    logger.info("Command: {}", pb.command());
            Process p = pb.start();
            int exitStatus = p.waitFor();
            return new ProcessResult(
			    exitStatus,
                    	    readInputStream(p.getInputStream()),
                            readInputStream(p.getErrorStream())
            );
        } catch( Exception e ) {
	    errorMessage = e.getMessage();
            logger.error("Error running ansible ingest process", e);
        }
        return new ProcessResult(-1, errorMessage, "");
    }

    @Override
    public void run() {
        while( !Thread.currentThread().isInterrupted() ) {
            try {
                Optional<Path> p = queue.take();
                if ( !p.isPresent() ) {
                    logger.info("Got shutdown signal");
                    break;
                }
                int count = processCount.incrementAndGet();
                logger.info("[{}] Received {}", count, p);
		ProcessResult result = runCommand(p.get());
		logger.info("[{}] result: {}", count, result);
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
