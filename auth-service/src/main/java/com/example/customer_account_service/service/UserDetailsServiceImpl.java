package com.example.customer_account_service.service;

import com.example.customer_account_service.model.Customer;
import com.example.customer_account_service.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    CustomerRepository customerRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Tải Customer Entity từ DB
        Customer customer = customerRepository.findByUsername(username) // Sử dụng findByUsername của CustomerRepository
                .orElseThrow(() -> new UsernameNotFoundException("Customer Not Found with username: " + username));

        // Customer Entity đã triển khai UserDetails
        return customer;
    }
}