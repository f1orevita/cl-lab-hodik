package com.cl_labs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class Processor {
    public static final int CMD_CREATE_PRODUCT = 10;
    public static final int CMD_READ_PRODUCT = 11;
    public static final int CMD_UPDATE_PRODUCT = 12;
    public static final int CMD_DELETE_PRODUCT = 13;
    public static final int CMD_SEARCH_PRODUCTS = 14;
    
    public static final int CMD_SUCCESS = 200;
    public static final int CMD_ERROR = 500;

    private final ProductService productService;
    private final ProtocolHandler protocolHandler;
    private final ObjectMapper objectMapper;

    public Processor(ProductService productService, ProtocolHandler protocolHandler) {
        this.productService = productService;
        this.protocolHandler = protocolHandler;
        this.objectMapper = new ObjectMapper();
    }

    public byte[] processMessage(DecodedMessage request) {
        try {
            int command = request.getcType();
            String requestJson = new String(request.getMessageData(), StandardCharsets.UTF_8);
            String responseJson = "";
            int responseType = CMD_SUCCESS;

            switch (command) {
                case CMD_CREATE_PRODUCT -> {
                    Product newProduct = objectMapper.readValue(requestJson, Product.class);
                    Product created = productService.create(newProduct);
                    responseJson = objectMapper.writeValueAsString(created);
                }
                case CMD_READ_PRODUCT -> {
                    int productId = Integer.parseInt(requestJson);
                    Optional<Product> product = productService.read(productId);
                    if (product.isPresent()) {
                        responseJson = objectMapper.writeValueAsString(product.get());
                    } else {
                        responseType = CMD_ERROR;
                        responseJson = "{\"error\": \"Товар не знайдено\"}";
                    }
                }
                case CMD_UPDATE_PRODUCT -> {
                    Product updateData = objectMapper.readValue(requestJson, Product.class);
                    productService.update(updateData);
                    responseJson = "{\"status\": \"Оновлено успішно\"}";
                }
                case CMD_DELETE_PRODUCT -> {
                    int deleteId = Integer.parseInt(requestJson);
                    productService.delete(deleteId);
                    responseJson = "{\"status\": \"Видалено успішно\"}";
                }
                case CMD_SEARCH_PRODUCTS -> {
                    ProductFilter filter = objectMapper.readValue(requestJson, ProductFilter.class);
                    List<Product> results = productService.search(filter);
                    responseJson = objectMapper.writeValueAsString(results);
                }
                default -> {
                    responseType = CMD_ERROR;
                    responseJson = "{\"error\": \"Невідома команда\"}";
                }
            }

            return protocolHandler.encode(
                (byte) 2,
                request.getbPktId(),
                responseType,
                request.getbUserId(),
                responseJson.getBytes(StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            e.printStackTrace();
            try {
                return protocolHandler.encode(
                    (byte) 2, request.getbPktId(), CMD_ERROR, request.getbUserId(),
                    ("{\"error\": \"" + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8)
                );
            } catch (Exception ex) {
                return new byte[0]; // Критична помилка шифрування
            }
        }
    }
}