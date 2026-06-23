package com.cl_labs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class EndToEndSystemTest {
    private Connection connection;
    private StoreServerTCP server;
    private StoreClientTCP client;
    private static final int E2E_PORT = 9095;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE products (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT, category TEXT, quantity INTEGER, price REAL)");
        }

        ProductService productService = new ProductService(connection);
        ProtocolHandler protocolHandler = new ProtocolHandler();
        Processor processor = new Processor(productService, protocolHandler);

        server = new StoreServerTCP();
        server.setProcessor(processor);
        server.start(E2E_PORT);

        client = new StoreClientTCP("127.0.0.1", E2E_PORT);
        
        Thread.sleep(200);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
        if (connection != null) connection.close();
    }

    @Test
    void testFullSystemFlow_CreateProduct() {
        assertDoesNotThrow(() -> {
            String jsonPayload = "{\"id\":0,\"name\":\"Ігровий ПК\",\"category\":\"Електроніка\",\"quantity\":2,\"price\":1500.0}";

            String responseJson = client.sendCommand(Processor.CMD_CREATE_PRODUCT, jsonPayload, 1L, 3);

            System.out.println("E2E Відповідь від сервера: " + responseJson);
            assertTrue(responseJson.contains("\"id\":1"), "Сервер мав присвоїти товару ID 1");
            assertTrue(responseJson.contains("Ігровий ПК"), "Назва товару має зберігатися");
        });
    }
    
    @Test
    void testFullSystemFlow_SearchProduct() {
        assertDoesNotThrow(() -> {
            client.sendCommand(Processor.CMD_CREATE_PRODUCT, "{\"id\":0,\"name\":\"Смартфон\",\"category\":\"Електроніка\",\"quantity\":10,\"price\":800.0}", 1L, 3);
            
            String searchJson = "{\"category\":\"Електроніка\", \"limit\":10, \"offset\":0}";
            String responseJson = client.sendCommand(Processor.CMD_SEARCH_PRODUCTS, searchJson, 2L, 3);
            
            System.out.println("E2E Відповідь на пошук: " + responseJson);
            assertTrue(responseJson.contains("Смартфон"));
        });
    }
}