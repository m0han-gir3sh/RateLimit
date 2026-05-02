package com.example.rateLimiter.filters;

import com.example.rateLimiter.exception.ApiErrorResponseWriter;
import com.example.rateLimiter.service.RateLimiter;
import com.example.rateLimiter.service.RateLimiterRouter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "userId";
    private static final String MISSING_USER = "User Id is missing";

    @Value("${rate.limit.windowSeconds}")
    private int windowSeconds;

    private final RateLimiterRouter rateLimiterRouter;
    private final ApiErrorResponseWriter errorResponseUtil;

    public RateLimitingFilter(RateLimiterRouter rateLimiterRouter,
                               ApiErrorResponseWriter errorResponseUtil) {
        this.rateLimiterRouter = rateLimiterRouter;
        this.errorResponseUtil = errorResponseUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
                
        String userId = extractUserId(request);

        if (!StringUtils.hasText(userId)) {
            errorResponseUtil.sendError(response,401,"Unauthorized",MISSING_USER);
            return;
        }

        if (!rateLimiterRouter.isAllowed(userId)) {
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            errorResponseUtil.sendError(response,429,"Too Many Requests","Rate limit exceeded. Try again after " + windowSeconds + " seconds");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractUserId(HttpServletRequest request) {
        return request.getHeader(USER_ID_HEADER);
    }
}