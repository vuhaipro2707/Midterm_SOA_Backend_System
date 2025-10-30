package com.example.customer_account_service.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "customers")
@Data // Lombok: Tự động tạo getters, setters, toString, v.v.
public class Customer implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId; // Primary Key

    @Column(unique = true, nullable = false)
    private String username; // Dùng cho đăng nhập

    @Column(nullable = false)
    private String password; // Mật khẩu đã mã hóa

    @Column(nullable = false)
    private String fullName; // Fullname (có dấu tiếng Việt)

    @Column(unique = true)
    private String phoneNumber; // Phone number (có số 0 đầu)

    @Column(nullable = false)
    private Integer availableBalance = 0; // Số dư khả dụng (VND, số nguyên)

    private String roles = "ROLE_USER"; // Vai trò mặc định

    // ----------------------------------------------------
    // Triển khai các phương thức của UserDetails
    // ----------------------------------------------------
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(roles));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}