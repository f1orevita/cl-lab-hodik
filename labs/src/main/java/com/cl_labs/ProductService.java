package com.cl_labs;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductService {
    private final Connection connection;

    public ProductService(Connection connection) {
        this.connection = connection;
    }

    public Product create(Product p) throws SQLException {
        String sql = "INSERT INTO products (name, category, quantity, price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, p.name());
            stmt.setString(2, p.category());
            stmt.setInt(3, p.quantity());
            stmt.setDouble(4, p.price());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Product(generatedKeys.getInt(1), p.name(), p.category(), p.quantity(), p.price());
                }
            }
        }
        throw new SQLException("Помилка створення товару");
    }

    public Optional<Product> read(int id) throws SQLException {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void update(Product p) throws SQLException {
        String sql = "UPDATE products SET name = ?, category = ?, quantity = ?, price = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, p.name());
            stmt.setString(2, p.category());
            stmt.setInt(3, p.quantity());
            stmt.setDouble(4, p.price());
            stmt.setInt(5, p.id());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public List<Product> search(ProductFilter filter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (filter.name() != null && !filter.name().isEmpty()) {
            sql.append(" AND name LIKE ?");
            params.add("%" + filter.name() + "%");
        }
        if (filter.category() != null && !filter.category().isEmpty()) {
            sql.append(" AND category = ?");
            params.add(filter.category());
        }
        if (filter.minQty() != null) {
            sql.append(" AND quantity >= ?");
            params.add(filter.minQty());
        }
        if (filter.maxQty() != null) {
            sql.append(" AND quantity <= ?");
            params.add(filter.maxQty());
        }
        if (filter.minPrice() != null) {
            sql.append(" AND price >= ?");
            params.add(filter.minPrice());
        }
        if (filter.maxPrice() != null) {
            sql.append(" AND price <= ?");
            params.add(filter.maxPrice());
        }

        // Пагінація
        sql.append(" LIMIT ? OFFSET ?");
        params.add(filter.limit());
        params.add(filter.offset());

        List<Product> results = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToProduct(rs));
                }
            }
        }
        return results;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        return new Product(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getInt("quantity"),
            rs.getDouble("price")
        );
    }
}