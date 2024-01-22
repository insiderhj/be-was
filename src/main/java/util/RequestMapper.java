package util;

import annotation.RequestBody;
import annotation.RequestMapping;
import annotation.RequestParam;
import constant.ParamType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webserver.HttpRequest;
import webserver.HttpResponse;
import constant.HttpStatus;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class RequestMapper {
    private static final Logger logger = LoggerFactory.getLogger(RequestMapper.class);
    public static final Map<String, Method> REQUEST_MAP = new HashMap<>();

    static {
        List<Class<?>> controllers = ClassScanner.scanControllers("controller");

        Class<? extends Annotation> requestMapping = RequestMapping.class;
        for (Class<?> c : controllers) {
            Method[] methods = c.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(requestMapping)) {
                    RequestMapping requestInfo = (RequestMapping) method.getAnnotation(requestMapping);
                    REQUEST_MAP.put(requestInfo.method() + " " + requestInfo.path(), method);
                }
            }
        }
    }

    public static Method getMethod(HttpRequest request) {
        return REQUEST_MAP.get(request.getMethod() + " " + request.getPath());
    }

    public static HttpResponse invoke(Method method, HttpRequest request) {
        try {
            Class<?> clazz = method.getDeclaringClass();
            Object instance = clazz.getDeclaredConstructor().newInstance();

            Object result = method.invoke(instance, mapParams(method, request));
            if (result instanceof HttpResponse) return (HttpResponse) result;
        } catch (IllegalArgumentException e) {
            return HttpResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage())
                    .build();
        } catch (RuntimeException | NoSuchMethodException
                 | InvocationTargetException | InstantiationException
                 | IllegalAccessException e) {
            logger.error(e.getMessage());
        }
        return HttpResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(HttpStatus.INTERNAL_SERVER_ERROR.getFullMessage())
                .build();
    }

    private static Object[] mapParams(Method method, HttpRequest request) {
        Parameter[] parameters = method.getParameters();
        Object[] params = new Object[parameters.length];

        IntStream.range(0, parameters.length)
                .forEach(i -> {
                    Parameter parameter = parameters[i];
                    if(parameter.isAnnotationPresent(RequestParam.class)){
                        ParamType paramType = ParamType.getByClass(parameter.getType());
                        RequestParam annotation = parameter.getAnnotation(RequestParam.class);
                        String requestParam = request.getParamMap().get(annotation.value());
                        if (requestParam != null) {
                            params[i] = paramType.map(requestParam);
                        } else if (!annotation.required()) {
                            params[i] = null;
                        } else {
                            throw new IllegalArgumentException("Parameter '" + annotation.value() + "' cannot be null");
                        }
                    } else if (parameter.isAnnotationPresent(RequestBody.class)) {
                        try {
                            Map<String, String> queryMap = RequestParser.parseQueryString(new String(request.getBody()));
                            params[i] = RequestParser.mapToClass(queryMap, parameter.getType());
                        } catch (UnsupportedEncodingException | InvocationTargetException | IllegalAccessException
                                | NoSuchMethodException | InstantiationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        return params;
    }
}
