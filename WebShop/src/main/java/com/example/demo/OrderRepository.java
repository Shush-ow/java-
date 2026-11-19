package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // ユーザーIDから購入履歴を取得（新しい注文順）
    List<Order> findByUserIdOrderByOrderedAtDesc(String userId);

}