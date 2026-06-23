package com.cl_labs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorTest {
    private Connection connection;
    private Processor processor;
    private ProtocolHandler protocolHandler;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE products (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT, category TEXT, quantity INTEGER, price REAL)");
        }

        ProductService productService = new ProductService(connection);
        protocolHandler = new ProtocolHandler();
        processor = new Processor(productService, protocolHandler);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    void testCreateProductCommand() throws Exception {
        String jsonPayload = "{\"id\":0,\"name\":\"Процесор Intel\",\"category\":\"Комплектуючі\",\"quantity\":10,\"price\":300.5}";
        
        DecodedMessage request = new DecodedMessage((byte)1, 1L, Processor.CMD_CREATE_PRODUCT, 101, jsonPayload.getBytes());

        byte[] rawResponse = processor.processMessage(request);

        DecodedMessage response = protocolHandler.decode(rawResponse);
        
        assertEquals(Processor.CMD_SUCCESS, response.getcType());
        String responseBody = new String(response.getMessageData());
        assertTrue(responseBody.contains("\"id\":1"));
        assertTrue(responseBody.contains("Процесор Intel"));
    }
}