package com.example.payment_processor_service.repository;

import com.example.payment_processor_service.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTuitionId(Long tuitionId);
    List<PaymentTransaction> findByCustomerId(Long customerId);
}