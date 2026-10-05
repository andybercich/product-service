package com.example.product_service.Handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class InternalCallInterceptor implements HandlerInterceptor {

    @Value("${internal.secret}")
    private String internalSecret;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        String headerSecret = request.getHeader("X-Internal-Secret");

        if (headerSecret == null || !headerSecret.equals(internalSecret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        String traceId = request.getHeader("X-Trace-Id");

        if (traceId != null) {
            MDC.put("traceId", traceId);
        } else {
            MDC.put("traceId", UUID.randomUUID().toString());
        }

        return true;
    }
}