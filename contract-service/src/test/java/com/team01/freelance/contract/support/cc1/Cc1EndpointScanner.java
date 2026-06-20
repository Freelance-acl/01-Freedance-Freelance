package com.team01.freelance.contract.support.cc1;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class Cc1EndpointScanner {

    public record Endpoint(HttpMethod method, String path, boolean isPublic) {
    }

    private Cc1EndpointScanner() {
    }

    public static List<Endpoint> scan(ApplicationContext context) {
        RequestMappingHandlerMapping mapping = context.getBean(
                "requestMappingHandlerMapping",
                RequestMappingHandlerMapping.class
        );
        Set<Endpoint> endpoints = new TreeSet<>(
                Comparator.comparing(Endpoint::path).thenComparing(Endpoint::method));

        mapping.getHandlerMethods().forEach((info, handler) -> addEndpoints(info, handler, endpoints));

        return new ArrayList<>(endpoints);
    }

    private static void addEndpoints(
            RequestMappingInfo info, HandlerMethod handler, Set<Endpoint> endpoints) {
        if (handler.getBeanType().getName().contains("BasicErrorController")) {
            return;
        }
        Set<HttpMethod> methods = resolveMethods(info);
        for (String pattern : info.getDirectPaths()) {
            for (HttpMethod method : methods) {
                boolean isPublic = Cc1PublicEndpoints.isPublic(method, pattern);
                endpoints.add(new Endpoint(method, pattern, isPublic));
            }
        }
    }

    private static Set<HttpMethod> resolveMethods(RequestMappingInfo info) {
        Set<RequestMethod> requestMethods = info.getMethodsCondition().getMethods();
        if (requestMethods.isEmpty()) {
            return Set.of(HttpMethod.GET);
        }
        return requestMethods.stream().map(Cc1EndpointScanner::toHttpMethod).collect(java.util.stream.Collectors.toSet());
    }

    private static HttpMethod toHttpMethod(RequestMethod method) {
        return HttpMethod.valueOf(method.name());
    }

    /** Replace path variables with a sample id for MockMvc calls. */
    public static String samplePath(String path) {
        return path.replaceAll("\\{[^/]+}", "1");
    }

    public static MockHttpServletRequestBuilder mockRequest(Endpoint endpoint) {
        String path = samplePath(endpoint.path());
        HttpMethod method = endpoint.method();
        if (HttpMethod.POST.equals(method)) {
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}");
        }
        if (HttpMethod.PUT.equals(method)) {
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}");
        }
        if (HttpMethod.DELETE.equals(method)) {
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(path);
        }
        if (HttpMethod.PATCH.equals(method)) {
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}");
        }
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path);
    }
}
