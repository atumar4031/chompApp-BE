package com.decagon.chompapp.services;

import com.decagon.chompapp.dtos.OrderResponse;
import com.decagon.chompapp.dtos.ProductDto;
import com.decagon.chompapp.models.Order;
import com.decagon.chompapp.models.ProductImage;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AdminService {

    ResponseEntity<String> createProduct(ProductDto productDto);
    ResponseEntity<ProductImage> saveProductImage(String productImageURL, Long productId);
    ResponseEntity<String> updateProduct(ProductDto productDto, Long productId);
    ResponseEntity<ProductImage> updateProductImage(String productImageURL, Long productId);
    ResponseEntity<String> deleteProduct(Long productId);
    ResponseEntity<OrderResponse> viewAllOrders(int pageNo, int pageSize);


}
