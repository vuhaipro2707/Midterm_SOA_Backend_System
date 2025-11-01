package com.example.payment_processor_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, unique = true)
    private Long tuitionId;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private LocalDateTime paymentDate;
}