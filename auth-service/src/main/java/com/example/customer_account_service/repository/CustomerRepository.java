package com.example.customer_account_service.repository;

import com.example.customer_account_service.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Phương thức tìm kiếm người dùng theo username (bắt buộc cho Spring Security)
    Optional<Customer> findByUsername(String username);
}