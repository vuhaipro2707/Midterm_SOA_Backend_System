package com.example.payment_processor_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

record ServiceStatus(String service, String message) {}

@RestController
public class Test {

	@GetMapping("/test")
	public ServiceStatus testEndpoint() {
		return new ServiceStatus("payment-processor-service", "Payment Processor Service is running.");
	}
}
