package com.example.customer_management_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

record ServiceStatus(String service, String message) {}

@RestController
public class Test {

	@GetMapping("/test")
	public ServiceStatus testEndpoint() {
		return new ServiceStatus("customer-management-service", "Customer Management Service is running.");
	}
}
