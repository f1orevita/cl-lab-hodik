package com.cl_labs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceTest {
    private Connection connection;
    private ProductService service;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        service = new ProductService(connection);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE products (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT, category TEXT, quantity INTEGER, price REAL)");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    void testCreateAndRead() throws Exception {
        Product p = service.create(new Product(0, "Ноутбук", "Електроніка", 10, 1500.0));
        assertTrue(p.id() > 0);

        Optional<Product> readProduct = service.read(p.id());
        assertTrue(readProduct.isPresent());
        assertEquals("Ноутбук", readProduct.get().name());
    }

    @Test
    void testUpdateAndDelete() throws Exception {
        Product p = service.create(new Product(0, "Мишка", "Електроніка", 50, 25.0));
        
        service.update(new Product(p.id(), "Мишка Бездротова", "Електроніка", 45, 30.0));
        assertEquals("Мишка Бездротова", service.read(p.id()).get().name());

        service.delete(p.id());
        assertFalse(service.read(p.id()).isPresent());
    }

    @Test
    void testDynamicSearchAndPagination() throws Exception {
        service.create(new Product(0, "Молоко", "Їжа", 100, 2.5));
        service.create(new Product(0, "Хліб", "Їжа", 50, 1.2));
        service.create(new Product(0, "Сир", "Їжа", 20, 5.0));
        service.create(new Product(0, "Клавіатура", "Електроніка", 15, 45.0));
        service.create(new Product(0, "Монітор", "Електроніка", 5, 200.0));

        ProductFilter filter1 = new ProductFilter(null, "Їжа", null, null, 3.0, null, 10, 0);
        List<Product> result1 = service.search(filter1);
        assertEquals(1, result1.size());
        assertEquals("Сир", result1.get(0).name());

        ProductFilter filter2 = new ProductFilter(null, null, null, null, null, null, 2, 2);
        List<Product> result2 = service.search(filter2);
        assertEquals(2, result2.size());
        assertEquals("Сир", result2.get(0).name());
        assertEquals("Клавіатура", result2.get(1).name());
    }
}