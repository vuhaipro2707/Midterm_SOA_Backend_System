package com.example.customer_account_service.config;

import com.example.customer_account_service.model.Customer;
import com.example.customer_account_service.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initializeData() {
        return args -> {
            // Chỉ tạo dữ liệu nếu bảng Customer trống
            if (customerRepository.count() == 0) {
                
                // 1. Tạo danh sách các khách hàng mẫu
                List<Customer> initialCustomers = Arrays.asList(
                    // Khách hàng 1: user có vai trò mặc định
                    createCustomer("user1", "123", "Vũ Thanh Hải", "vuhaipro2707@gmail.com", "0901234567", 5000000, "ROLE_USER"),
                    createCustomer("user2", "1234", "Huỳnh Đặng Tấn Phát", "vuhaipro1234231@gmail.com", "0918765432", 3000000, "ROLE_USER"),
                    createCustomer("user3", "12345", "Nguyễn Anh Quốc", "vuhaipro12342312.com", "0987654321", 1000000, "ROLE_USER")
                );

                // 2. Lưu toàn bộ danh sách vào cơ sở dữ liệu
                customerRepository.saveAll(initialCustomers);
                
                System.out.println("--- Successfully created " + initialCustomers.size() + " test customers in the database. ---");
            }
        };
    }
    
    // Phương thức helper để tạo đối tượng Customer
    private Customer createCustomer(String username, String rawPassword, String fullName, String email, String phoneNumber, Integer balance, String roles) {
        Customer customer = new Customer();
        customer.setUsername(username);
        customer.setPassword(passwordEncoder.encode(rawPassword)); 
        customer.setFullName(fullName);
        customer.setEmail(email);
        customer.setPhoneNumber(phoneNumber);
        customer.setAvailableBalance(balance);
        customer.setRoles(roles);
        return customer;
    }
}