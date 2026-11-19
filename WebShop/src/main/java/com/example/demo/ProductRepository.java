package com.example.demo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
        extends JpaRepository<Product, Integer> {

	@Query(value = "SELECT * FROM products WHERE id = :id FOR UPDATE", nativeQuery = true)
	Optional<Product> findByIdForUpdate(@Param("id") Integer id);
}