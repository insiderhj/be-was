package webserver;

import constant.HttpHeader;
import constant.HttpStatus;
import constant.MimeType;
import exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.html.HtmlBuilder;
import util.web.SharedData;
import util.session.SessionManager;
import util.web.RequestMapper;
import util.web.ResourceLoader;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Objects;

public class RequestHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private final Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            HttpResponse response;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                HttpRequest request = new HttpRequest(reader);
                SharedData.request.set(request);
                SharedData.requestUser.set(SessionManager.getLoggedInUser(request));

                logger.debug("Connection IP : {}, Port : {}, request: {}",
                        connection.getInetAddress(), connection.getPort(), request.getPath());

                Method handler = RequestMapper.getMethod(request);
                if (handler != null) {
                    response = RequestMapper.invoke(handler);
                } else if (Objects.equals(request.getMethod(), "GET")) {
                    byte[] fileContent = ResourceLoader.getFileContent(request.getPath());

                    // html 파일이면 동적으로 내용 변경
                    if (MimeType.HTML.getMimeType().equals(ResourceLoader.getMimeType(request.getPath())))
                        fileContent = HtmlBuilder.process(fileContent);

                    response = HttpResponse.builder()
                            .status(HttpStatus.OK)
                            .addHeader(HttpHeader.CONTENT_TYPE, ResourceLoader.getMimeType(request.getPath()))
                            .body(fileContent)
                            .build();
                } else
                    throw new ResourceNotFoundException(request.getPath());

            } catch (ResourceNotFoundException e) {
                response = HttpResponse.of(HttpStatus.NOT_FOUND);
            } catch (IllegalArgumentException | IndexOutOfBoundsException | NoSuchMethodException
                     | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                response = HttpResponse.of(HttpStatus.BAD_REQUEST);
            } catch (IllegalStateException | IOException e) {
                logger.error("error processing request: {}", e.getMessage());
                response = HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            response.send(out, logger);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
