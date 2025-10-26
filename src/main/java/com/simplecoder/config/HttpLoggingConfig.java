package com.simplecoder.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Configuration to intercept and log raw HTTP requests/responses
 * sent to the OpenAI API (or compatible endpoints).
 * 
 * This enables visibility into each turn of the ReAct loop at the HTTP layer.
 */
@Slf4j
@Configuration
public class HttpLoggingConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .requestInterceptor(new LoggingInterceptor());
    }

    /**
     * Interceptor that logs request and response details for OpenAI API calls.
     */
    private static class LoggingInterceptor implements ClientHttpRequestInterceptor {
        
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingInterceptor.class);

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                             ClientHttpRequestExecution execution) throws IOException {
            // Log request
            logRequest(request, body);

            // Execute the request
            ClientHttpResponse response = execution.execute(request, body);

            // Wrap response to allow reading body multiple times
            BufferedClientHttpResponse bufferedResponse = new BufferedClientHttpResponse(response);
            
            // Log response
            logResponse(bufferedResponse);

            return bufferedResponse;
        }

        private void logRequest(HttpRequest request, byte[] body) {
            log.info("=== HTTP Request to LLM ===");
            log.info("URI: {} {}", request.getMethod(), request.getURI());
            log.info("Headers: {}", request.getHeaders());
            if (body.length > 0) {
                String bodyStr = new String(body, StandardCharsets.UTF_8);
                log.info("Request Body: {}", bodyStr);
            }
        }

        private void logResponse(BufferedClientHttpResponse response) throws IOException {
            log.info("=== HTTP Response from LLM ===");
            log.info("Status: {}", response.getStatusCode().value());
            log.info("Headers: {}", response.getHeaders());
            
            String bodyStr = new String(response.getBodyBytes(), StandardCharsets.UTF_8);
            if (!bodyStr.isEmpty()) {
                log.info("Response Body: {}", bodyStr);
            }
        }
    }

    /**
     * Wrapper that buffers the response body so it can be read multiple times.
     */
    private static class BufferedClientHttpResponse implements ClientHttpResponse {
        
        private final ClientHttpResponse response;
        private byte[] body;

        public BufferedClientHttpResponse(ClientHttpResponse response) throws IOException {
            this.response = response;
            // Read and buffer the body once
            this.body = StreamUtils.copyToByteArray(response.getBody());
        }

        public byte[] getBodyBytes() {
            return this.body;
        }

        @Override
        public InputStream getBody() throws IOException {
            // Return a new stream from the buffered bytes
            return new ByteArrayInputStream(this.body);
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return response.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return response.getStatusText();
        }

        @Override
        public void close() {
            response.close();
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return response.getHeaders();
        }
    }
}
