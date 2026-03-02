package com.cvr.cse.lecturesummarizer.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@WebFilter("/*")
public class CorsFilter implements Filter {

    public CorsFilter() {
        System.out.println("=== CORS FILTER LOADED ===");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        // Log every request to see if the filter is actually running
        System.out.println("CORS Filter Processing: " + request.getMethod() + " " + request.getRequestURI());

        // Allow your specific origins
        String origin = request.getHeader("Origin");
        if (origin != null && (
            origin.equals("https://lecsumm.indevs.in") ||
            origin.equals("https://lecsumm.vercel.app") ||
            origin.equals("http://localhost:5173"))) {

            response.setHeader("Access-Control-Allow-Origin", origin);
            System.out.println("CORS Origin Allowed: " + origin);
        } else {
            System.out.println("CORS Origin Rejected/Null: " + origin);
        }

        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, x-requested-with, Accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");

        // Handle preflight requests immediately
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            System.out.println("Responding to OPTIONS request with status 200");
            response.setStatus(HttpServletResponse.SC_OK);
            return; // Don't pass to the rest of the filter chain for OPTIONS
        }

        // Pass the request down the chain for non-OPTIONS requests
        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("=== CORS FILTER INITIALIZED ===");
    }

    @Override
    public void destroy() {
        System.out.println("=== CORS FILTER DESTROYED ===");
    }
}