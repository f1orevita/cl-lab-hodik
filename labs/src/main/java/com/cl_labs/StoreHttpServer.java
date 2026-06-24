package com.cl_labs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StoreHttpServer {
    private HttpServer server;
    private final ProductService productService;
    private final ObjectMapper mapper = new ObjectMapper();

    public StoreHttpServer(ProductService productService) {
        this.productService = productService;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/login", new LoginHandler());
        server.createContext("/products", new ProductsHandler());
        
        server.setExecutor(null);
        server.start();
        System.out.println("HTTP Сервер запущено на порту " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    Map<String, String> credentials = mapper.readValue(exchange.getRequestBody(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                    String login = credentials.get("login");
                    String password = credentials.get("password");

                    if ("admin".equals(login) && "admin123".equals(password)) {
                        String token = JwtUtils.generateToken(login);
                        sendResponse(exchange, 200, "{\"token\":\"" + token + "\"}");
                    } else {
                        sendResponse(exchange, 401, "{\"error\":\"Невірний логін або пароль\"}");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"Невірний формат JSON\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    private class ProductsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendResponse(exchange, 401, "{\"error\":\"Відсутній або невірний Authorization заголовок\"}");
                return;
            }
            String token = authHeader.substring(7);
            if (!JwtUtils.verifyToken(token)) {
                sendResponse(exchange, 403, "{\"error\":\"Токен недійсний або прострочений\"}");
                return;
            }

            // ОБРОБКА REST МЕТОДІВ
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] pathParts = path.split("/"); // [ "", "products", "id" ]
            
            try {
                switch (method) {
                    case "GET" -> {
                        if (pathParts.length == 3) {
                            int id = Integer.parseInt(pathParts[2]);
                            Optional<Product> p = productService.read(id);
                            if (p.isPresent()) sendResponse(exchange, 200, mapper.writeValueAsString(p.get()));
                            else sendResponse(exchange, 404, "{\"error\":\"Товар не знайдено\"}");
                        }
                    }
                    case "PUT" -> {
                        Product p = mapper.readValue(exchange.getRequestBody(), Product.class);
                        List<Product> existing = productService.search(new ProductFilter(p.name(), null, null, null, null, null, 1, 0));
                        if (!existing.isEmpty() && existing.get(0).name().equalsIgnoreCase(p.name())) {
                            sendResponse(exchange, 409, "{\"error\":\"Продукт з таким іменем вже існує\"}");
                            return;
                        }
                        Product created = productService.create(p);
                        sendResponse(exchange, 201, mapper.writeValueAsString(created));
                    }
                    case "POST" -> {
                        if (pathParts.length == 3) {
                            int id = Integer.parseInt(pathParts[2]);
                            Product updateData = mapper.readValue(exchange.getRequestBody(), Product.class);
                            Product toUpdate = new Product(id, updateData.name(), updateData.category(), updateData.quantity(), updateData.price());
                            productService.update(toUpdate);
                            sendResponse(exchange, 200, "{\"status\":\"Оновлено\"}");
                        }
                    }
                    case "DELETE" -> {
                        if (pathParts.length == 3) {
                            int id = Integer.parseInt(pathParts[2]);
                            productService.delete(id);
                            sendResponse(exchange, 200, "{\"status\":\"Видалено\"}");
                        }
                    }
                    default -> sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"Помилка сервера: " + e.getMessage() + "\"}");
            }
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}