package com.cl_labs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkUDPTest {
    private StoreServerUDP server;
    private StoreClientUDP client;
    private static final int TEST_PORT = 9091;

    @BeforeEach
    void setUp() throws Exception {
        server = new StoreServerUDP();
        server.start(TEST_PORT);
        client = new StoreClientUDP("127.0.0.1", TEST_PORT);
        Thread.sleep(100);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void testSuccessfulUdpCommunication() {
        assertDoesNotThrow(() -> {
            String result = client.sendWithRetry("Тест UDP доставки", 1, 3);
            assertEquals("Success", result);
        });
    }

    @Test
    void testUdpRetryMechanism() {
        server.setSimulatePacketLoss(true);

        assertDoesNotThrow(() -> {
            String result = client.sendWithRetry("Тест переповтору UDP", 2, 3);
            assertEquals("Success", result);
        });
    }

    @Test
    void testUdpMaxRetriesExceeded() {
        // Повністю вимикаємо сервер
        server.stop();

        Exception exception = assertThrows(Exception.class, () -> {
            // Робимо 2 спроби, обидві мають провалитися
            client.sendWithRetry("Повідомлення в чорну діру", 3, 2);
        });

        // Перевіряємо, що клієнт здався і викинув правильну помилку
        assertTrue(exception.getMessage().contains("Не вдалося доставити UDP пакет"));
    }
}