package com.cloud.ApiGateway;

import java.util.List;

import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class SecurityFilter implements GlobalFilter,Ordered {


	    @Autowired
	    private WebClient.Builder webClientBuilder;

	    private static final List<String> openApiEndpoints = List.of(
	            "/api/auth/login",
	            "/api/auth/signup"
	    );

	    @Override
	    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

	        ServerHttpRequest request = exchange.getRequest();
	        String path = request.getURI().getPath();
	        
	        System.out.println("🔍 Incoming request path: " + path);

	        // Allow open endpoints without auth
	        if (openApiEndpoints.stream().anyMatch(path::contains)) {
	            return chain.filter(exchange);
	        }

	        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
	        
	        System.out.println("🔐 Auth Header: " + authHeader);

	        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
	        	 System.out.println("❌ Missing or invalid Authorization header");
	            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
	            return exchange.getResponse().setComplete();
	        }

	        return webClientBuilder.build()
	                .get()
	                .uri("http://security-microservice/api/auth/validate")
	                .header(HttpHeaders.AUTHORIZATION, authHeader)
	                .retrieve()
	                
	                .bodyToMono(String.class)
	                .flatMap(response -> {
	                    // Token is valid → continue to downstream service
	                    return chain.filter(exchange);
	                })
	                .onErrorResume(error -> {
	                	 System.out.println("❌ Error occurred: " + error.getMessage());
	                	exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
	                    return exchange.getResponse().setComplete();
	                });
	    }

	    @Override
	    public int getOrder() {
	        return -1; // Ensures this filter runs before others
	    }
	

}
