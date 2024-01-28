package webserver;

import constant.HttpHeader;
import util.web.RequestParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private final String method;
    private final String path;
    private final Map<String, String> paramMap;
    private final Map<HttpHeader, String> header;
    private final char[] body;

    public HttpRequest(BufferedReader reader) throws IOException {
        String requestLine = reader.readLine();
        String[] requestParts = requestLine.split(" ");
        this.method = requestParts[0].toUpperCase();
        this.path = RequestParser.extractPath(requestParts[1]);
        this.paramMap = RequestParser.parseQueryString(RequestParser.extractQuery(requestParts[1]));
        this.header = new HashMap<>();

        String s;
        while ((s = reader.readLine()) != null && !s.isEmpty()) {
            requestParts = s.split(":\\s*", 2);
            if (requestParts.length == 2) {
                try {
                    this.header.put(HttpHeader.of(requestParts[0]), requestParts[1]);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (header.get(HttpHeader.CONTENT_LENGTH) != null) {
            body = new char[Integer.parseInt(header.get(HttpHeader.CONTENT_LENGTH))];
            reader.read(body);
        }else
            body = new char[0];
    }

    public String getMethod() {
        return this.method;
    }

    public String getPath() {
        return this.path;
    }

    public Map<String, String> getParamMap() {
        return this.paramMap;
    }

    public Map<HttpHeader, String> getHeader() {
        return this.header;
    }

    public char[] getBody() {
        return this.body;
    }
}
