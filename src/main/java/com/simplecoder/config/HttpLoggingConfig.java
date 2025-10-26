package com.simplecoder.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuration to intercept and log raw HTTP requests/responses
 * sent to the OpenAI API (or compatible endpoints).
 *
 * Provides optional pretty-printed JSON, ANSI colors, truncation safeguards,
 * and header masking (Authorization).
 */
@Slf4j
@Configuration
public class HttpLoggingConfig {

    @Value("${simple-coder.logging.http.enabled:true}")
    private boolean enabled;

    @Value("${simple-coder.logging.http.pretty-json:true}")
    private boolean prettyJson;

    @Value("${simple-coder.logging.http.ansi-colors:true}")
    private boolean ansiColors;

    @Value("${simple-coder.logging.http.max-body-chars:10000}")
    private int maxBodyChars;

    private final ObjectMapper mapper = new ObjectMapper();

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .requestInterceptor(new LoggingInterceptor());
    }

    /**
     * Interceptor that logs request and response details for OpenAI API calls.
     */
    private class LoggingInterceptor implements ClientHttpRequestInterceptor {

        private final org.slf4j.Logger httpLog = org.slf4j.LoggerFactory.getLogger(LoggingInterceptor.class);

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            if (enabled) {
                logRequest(request, body);
            }
            ClientHttpResponse response = execution.execute(request, body);
            BufferedClientHttpResponse bufferedResponse = new BufferedClientHttpResponse(response);
            if (enabled) {
                logResponse(bufferedResponse);
            }
            return bufferedResponse;
        }

        private void logRequest(HttpRequest request, byte[] body) {
            httpLog.info(color("=== HTTP Request to LLM ===", Ansi.MAGENTA));
            httpLog.info(color("URI: " + request.getMethod() + " " + request.getURI(), Ansi.YELLOW));
            httpLog.info(color("Headers: " + maskHeaders(request.getHeaders()), Ansi.CYAN));
            if (body.length > 0) {
                String bodyStr = new String(body, StandardCharsets.UTF_8);
                httpLog.info(color("Request Body:" + System.lineSeparator() + formatBody(bodyStr, request.getHeaders()), Ansi.GREEN));
            }
        }

        private void logResponse(BufferedClientHttpResponse response) throws IOException {
            httpLog.info(color("=== HTTP Response from LLM ===", Ansi.MAGENTA));
            httpLog.info(color("Status: " + response.getStatusCode().value(), Ansi.YELLOW));
            httpLog.info(color("Headers: " + maskHeaders(response.getHeaders()), Ansi.CYAN));
            String bodyStr = new String(response.getBodyBytes(), StandardCharsets.UTF_8);
            if (!bodyStr.isEmpty()) {
                httpLog.info(color("Response Body:" + System.lineSeparator() + formatBody(bodyStr, response.getHeaders()), Ansi.GREEN));
            }
        }

        private String formatBody(String raw, HttpHeaders headers) {
            String processed = raw;
            if (isJson(headers, raw) && prettyJson) {
                try {
                    JsonNode node = mapper.readTree(raw);
                    processed = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
                } catch (JsonProcessingException e) {
                    // keep raw if parsing fails
                }
            }
            processed = truncate(processed, maxBodyChars);
            return processed;
        }

        private boolean isJson(HttpHeaders headers, String body) {
            String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
            if (contentType != null && contentType.toLowerCase().contains("json")) {
                return true;
            }
            char firstNonWs = body.chars()
                    .mapToObj(c -> (char) c)
                    .filter(ch -> !Character.isWhitespace(ch))
                    .findFirst()
                    .orElse('\0');
            return firstNonWs == '{' || firstNonWs == '[';
        }

        private String maskHeaders(HttpHeaders headers) {
            Map<String, String> masked = headers.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> maskHeaderValue(e.getKey(), e.getValue())));
            return masked.toString();
        }

        private String maskHeaderValue(String key, java.util.List<String> values) {
            if ("Authorization".equalsIgnoreCase(key) && !values.isEmpty()) {
                return values.stream().map(v -> v.length() <= 12 ? "(masked)" : v.substring(0, 12) + "***").collect(Collectors.joining(","));
            }
            return String.join(",", values);
        }

        private String truncate(String text, int max) {
            if (text.length() <= max) {
                return text;
            }
            return text.substring(0, max) + System.lineSeparator() + "... (truncated " + max + "/" + text.length() + ")";
        }

        private String color(String text, Ansi code) {
            if (!ansiColors) {
                return text;
            }
            return code.code + text + Ansi.RESET.code;
        }
    }

    /**
     * Wrapper that buffers the response body so it can be read multiple times.
     */
    private static class BufferedClientHttpResponse implements ClientHttpResponse {

        private final ClientHttpResponse response;
        private final byte[] body;

        public BufferedClientHttpResponse(ClientHttpResponse response) throws IOException {
            this.response = response;
            this.body = StreamUtils.copyToByteArray(response.getBody());
        }

        public byte[] getBodyBytes() {
            return this.body;
        }

        @Override
        public InputStream getBody() {
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
        public HttpHeaders getHeaders() {
            return response.getHeaders();
        }
    }

    /**
     * ANSI color codes for optional colored logging output.
     */
    private enum Ansi {
        RESET("\u001B[0m"),
        MAGENTA("\u001B[35m"),
        YELLOW("\u001B[33m"),
        CYAN("\u001B[36m"),
        GREEN("\u001B[32m");
        final String code;
        Ansi(String code) { this.code = code; }
    }
}
