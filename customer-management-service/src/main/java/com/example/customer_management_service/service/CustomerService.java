package com.example.customer_management_service.service;

import com.example.customer_management_service.model.Customer;
import com.example.customer_management_service.repository.CustomerRepository;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public Optional<Customer> getCustomerById(Long customerId) {
        return customerRepository.findByCustomerId(customerId);
    }

    public Integer getAvailableBalance(Long customerId) {
        return customerRepository.findByCustomerId(customerId)
                .map(Customer::getAvailableBalance)
                .orElse(null);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Integer debitCustomerBalance(Long customerId, Integer amount) {
        Optional<Customer> customerOpt = customerRepository.findByCustomerId(customerId);
        
        if (customerOpt.isEmpty()) {
            return null;
        }

        Customer customer = customerOpt.get();
        Integer currentBalance = customer.getAvailableBalance();
        
        if (currentBalance < amount) {
            throw new IllegalArgumentException("Insufficient balance to perform the debit. Required " + amount + ", available " + currentBalance + ".");
        }
        
        Integer newBalance = currentBalance - amount;
        customer.setAvailableBalance(newBalance);
        customerRepository.save(customer);

        return newBalance;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Integer creditCustomerBalance(Long customerId, Integer amount) {
        Optional<Customer> customerOpt = customerRepository.findByCustomerId(customerId);
        
        if (customerOpt.isEmpty()) {
            return null;
        }

        Customer customer = customerOpt.get();
        Integer currentBalance = customer.getAvailableBalance();
        
        Integer newBalance = currentBalance + amount;
        customer.setAvailableBalance(newBalance);
        customerRepository.save(customer);

        return newBalance;
    }
}