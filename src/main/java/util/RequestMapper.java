package util;

import annotation.RequestMapping;
import annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webserver.HttpRequest;
import webserver.HttpResponse;
import webserver.HttpStatus;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
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
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            logger.error(e.getMessage());
        }
        return HttpResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(HttpStatus.INTERNAL_SERVER_ERROR.getFullMessage())
                .build();
    }

    private static Object[] mapParams(Method method, HttpRequest request) {
        Class<? extends Annotation> requestParam = RequestParam.class;
        Parameter[] parameters = method.getParameters();
        Object[] params = new Object[parameters.length];

        IntStream.range(0, parameters.length)
                .forEach(i -> {
                    Parameter parameter = parameters[i];
                    if(parameter.isAnnotationPresent(requestParam)){
                        RequestParam annotation = (RequestParam) parameter.getAnnotation(requestParam);

                        params[i]= request.getParamMap().get(annotation.value());
                    }
                });

        return params;
    }
}
