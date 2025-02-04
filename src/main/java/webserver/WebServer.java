package webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    private static final int DEFAULT_PORT = 8080;
    private static final int maxThreads = 10;

    public static void main(String args[]) throws Exception {
        int port = 0;
        if (args == null || args.length == 0) {
            port = DEFAULT_PORT;
        } else {
            port = Integer.parseInt(args[0]);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);
        try (ServerSocket listenSocket = new ServerSocket(port)) {
            logger.info("Web Application Server started on {} port.", port);

            while (true) {
                Socket connection = listenSocket.accept();
                executorService.submit(new RequestHandler(connection));
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }
}
