package com.example.tuition_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

record ServiceStatus(String service, String message) {}

@RestController
public class Test {

	@GetMapping("/test")
	public ServiceStatus testEndpoint() {
		return new ServiceStatus("tuition-service", "Tuition Service is running.");
	}
}
