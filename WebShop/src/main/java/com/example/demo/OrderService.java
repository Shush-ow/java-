package com.example.demo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository detailRepository;
    private final ProductRepository productRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderDetailRepository detailRepository,
            ProductRepository productRepository) {

        this.orderRepository = orderRepository;
        this.detailRepository = detailRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void purchase(
            String userId,
            List<CartItem> cart,
            String paymentMethod) {

        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("カートが空です。");
        }

        if (!List.of("credit", "bank", "convenience")
                .contains(paymentMethod)) {
            throw new IllegalArgumentException("決済方法が不正です。");
        }

        int totalAmount = 0;
        List<Product> products = new ArrayList<>();

        // 商品情報と在庫を確認
        for (CartItem item : cart) {

            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("数量が不正です。");
            }

            Product product = productRepository
                    .findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() ->
                        new IllegalArgumentException("商品が存在しません。")
                    );

            if (product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException(
                    product.getName() + "の在庫が不足しています。"
                );
            }

            totalAmount = Math.addExact(
                totalAmount,
                Math.multiplyExact(
                    product.getPrice(),
                    item.getQuantity()
                )
            );

            products.add(product);
        }

        // 注文情報を登録
        Order order = new Order();

        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("pending");
        order.setPaymentMethod(paymentMethod);
        order.setOrderedAt(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());

        order = orderRepository.saveAndFlush(order);

        // 商品明細を登録・在庫更新
        for (int i = 0; i < cart.size(); i++) {

            CartItem item = cart.get(i);
            Product product = products.get(i);

            int quantity = item.getQuantity();
            int price = product.getPrice();
            int subtotal = Math.multiplyExact(price, quantity);

            OrderDetail detail = new OrderDetail();

            detail.setOrderId(order.getId());
            detail.setProductId(product.getId());
            detail.setProductName(product.getName());
            detail.setUnitPrice(price);
            detail.setQuantity(quantity);
            detail.setSubtotal(subtotal);

            detailRepository.save(detail);

            product.setStock(product.getStock() - quantity);
        }
    }
}