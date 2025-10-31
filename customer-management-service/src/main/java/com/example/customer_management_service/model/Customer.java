package com.example.customer_management_service.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "customers")
@Data 
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId; 

    @Column(unique = true, nullable = false)
    private String username; 

    @Column(nullable = false)
    private String password; 

    @Column(nullable = false)
    private String fullName; 

    @Column(nullable = false)
    private String email;
    
    @Column(unique = true)
    private String phoneNumber; 

    @Column(nullable = false)
    private Integer availableBalance = 0; 

    private String roles = "ROLE_USER"; 
}