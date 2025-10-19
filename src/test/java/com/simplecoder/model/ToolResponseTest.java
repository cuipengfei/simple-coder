package com.simplecoder.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSuccessWithMessageAndData() {
        Map<String, Object> data = Map.of("file", "test.txt", "lines", 42);
        ToolResponse response = ToolResponse.success("File read successfully", data);

        assertTrue(response.isSuccess());
        assertEquals("File read successfully", response.getMessage());
        assertEquals(data, response.getData());
        assertNull(response.getError());
    }

    @Test
    void testSuccessWithMessageOnly() {
        ToolResponse response = ToolResponse.success("Operation completed");

        assertTrue(response.isSuccess());
        assertEquals("Operation completed", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getError());
    }

    @Test
    void testErrorWithMessageAndDetails() {
        ToolResponse response = ToolResponse.error(
                "File not found",
                "java.io.FileNotFoundException: /path/to/file.txt"
        );

        assertFalse(response.isSuccess());
        assertEquals("File not found", response.getMessage());
        assertNull(response.getData());
        assertEquals("java.io.FileNotFoundException: /path/to/file.txt", response.getError());
    }

    @Test
    void testErrorWithMessageOnly() {
        ToolResponse response = ToolResponse.error("Invalid input");

        assertFalse(response.isSuccess());
        assertEquals("Invalid input", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getError());
    }

    @Test
    void testBuilderWithAllFields() {
        ToolResponse response = ToolResponse.builder()
                .success(true)
                .message("Custom message")
                .data(List.of("item1", "item2"))
                .error("Should not be present in success")
                .build();

        assertTrue(response.isSuccess());
        assertEquals("Custom message", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("Should not be present in success", response.getError());
    }

    @Test
    void testNoArgsConstructor() {
        ToolResponse response = new ToolResponse();

        assertFalse(response.isSuccess()); // Default boolean value is false
        assertNull(response.getMessage());
        assertNull(response.getData());
        assertNull(response.getError());
    }

    @Test
    void testAllArgsConstructor() {
        Map<String, String> data = Map.of("key", "value");
        ToolResponse response = new ToolResponse(
                true,
                "Test message",
                data,
                "Test error"
        );

        assertTrue(response.isSuccess());
        assertEquals("Test message", response.getMessage());
        assertEquals(data, response.getData());
        assertEquals("Test error", response.getError());
    }

    @Test
    void testSettersAndGetters() {
        ToolResponse response = new ToolResponse();

        response.setSuccess(true);
        response.setMessage("Updated message");
        response.setData("String data");
        response.setError("Error info");

        assertTrue(response.isSuccess());
        assertEquals("Updated message", response.getMessage());
        assertEquals("String data", response.getData());
        assertEquals("Error info", response.getError());
    }

    @Test
    void testJsonSerialization() throws Exception {
        ToolResponse response = ToolResponse.success(
                "Search completed",
                List.of("result1", "result2")
        );

        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"message\":\"Search completed\""));
        assertTrue(json.contains("\"data\":[\"result1\",\"result2\"]"));
        assertFalse(json.contains("error")); // Should be excluded due to @JsonInclude(NON_NULL)
    }

    @Test
    void testJsonSerializationWithError() throws Exception {
        ToolResponse response = ToolResponse.error("Failed", "Stack trace");

        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("\"message\":\"Failed\""));
        assertTrue(json.contains("\"error\":\"Stack trace\""));
        assertFalse(json.contains("data")); // Should be excluded due to @JsonInclude(NON_NULL)
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = """
                {
                    "success": true,
                    "message": "Test",
                    "data": {"key": "value"}
                }
                """;

        ToolResponse response = objectMapper.readValue(json, ToolResponse.class);

        assertTrue(response.isSuccess());
        assertEquals("Test", response.getMessage());
        assertNotNull(response.getData());
        assertNull(response.getError());
    }

    @Test
    void testDataTypeFlexibility() {
        // String data
        ToolResponse response1 = ToolResponse.success("Message", "String data");
        assertEquals("String data", response1.getData());

        // List data
        ToolResponse response2 = ToolResponse.success("Message", List.of(1, 2, 3));
        assertEquals(List.of(1, 2, 3), response2.getData());

        // Map data
        ToolResponse response3 = ToolResponse.success("Message", Map.of("k", "v"));
        assertEquals(Map.of("k", "v"), response3.getData());

        // Complex object
        ToolRequest complexData = ToolRequest.builder().prompt("test").build();
        ToolResponse response4 = ToolResponse.success("Message", complexData);
        assertEquals(complexData, response4.getData());
    }
}
