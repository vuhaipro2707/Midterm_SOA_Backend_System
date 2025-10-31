package com.example.payment_processor_service.service;

import com.example.payment_processor_service.dto.GenericResponse;
import com.example.payment_processor_service.dto.PaymentRequest;
import com.example.payment_processor_service.model.PaymentTransaction;
import com.example.payment_processor_service.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private WebClient webClient;
    
    private static final String CUSTOMER_SERVICE_BASE_URL = "http://customer-management-service:8082";
    private static final String TUITION_SERVICE_BASE_URL = "http://tuition-service:8084";

    @Transactional(readOnly = true)
    public List<PaymentTransaction> getPaymentHistory(Long customerId) {
        return paymentTransactionRepository.findByCustomerId(customerId);
    }

    @Transactional
    public PaymentTransaction processPayment(Long customerId, PaymentRequest request) {
        if (paymentTransactionRepository.findByTuitionId(request.getTuitionId()).isPresent()) {
            throw new IllegalArgumentException("Tuition ID " + request.getTuitionId() + " has already been paid.");
        }

        Integer requiredAmount = null;
        try {
            Mono<GenericResponse<Object>> tuitionMono = createGetTuitionMono(customerId, request.getTuitionId());
            GenericResponse<Object> tuitionResponse = tuitionMono.block();
            if (tuitionResponse != null && tuitionResponse.isSuccess() && tuitionResponse.getData() != null) {
                Object responseData = tuitionResponse.getData();
                if (responseData instanceof Map) {
                    Object amountObj = ((Map<?, ?>)responseData).get("amount");
                    if (amountObj instanceof Number) {
                        requiredAmount = ((Number)amountObj).intValue(); 
                    }
                }
            } else {
                String message = tuitionResponse != null && tuitionResponse.getMessage() != null ? 
                                 tuitionResponse.getMessage() : 
                                 "Tuition details could not be retrieved.";
                throw new RuntimeException(message);
            }
        } catch (Exception e) {
            throw mapToPaymentFailure("Tuition Fetch Failed", e);
        }

        if (requiredAmount == null || !requiredAmount.equals(request.getAmount())) {
            String requiredAmountStr = requiredAmount != null ? requiredAmount.toString() : "N/A or Invalid Type";
            throw new IllegalArgumentException(
                "Payment amount mismatch or missing amount in tuition record. Required: " + requiredAmountStr + ", Provided: " + request.getAmount() + "."
            );
        }
        
        Mono<GenericResponse<Integer>> debitMono = createDebitMono(customerId, request);
        
        final Integer debitAmount = request.getAmount(); 

        try {
            debitMono.block(); 
            
        } catch (Exception e) {
            throw mapToPaymentFailure("Customer Debit Failed", e);
        }

        Mono<GenericResponse<Object>> tuitionUpdateMono = createTuitionUpdateMono(customerId, request, true);

        try {
            tuitionUpdateMono.block(); 
            
        } catch (Exception e) {
            compensateCustomerDebit(customerId, debitAmount, request.getTuitionId());
             
            throw mapToPaymentFailure("Tuition Update Failed", e);
        }
        
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setCustomerId(customerId);
        transaction.setTuitionId(request.getTuitionId());
        transaction.setPaymentDate(LocalDateTime.now());
        
        return paymentTransactionRepository.save(transaction);
    }
    
    
    private void compensateCustomerDebit(Long customerId, Integer amount, Long tuitionId) {
        System.err.println("[COMPENSATION] Tuition Update failed. Attempting to CREDIT back amount " + amount + " for customer ID: " + customerId);
        
        Mono<GenericResponse<Integer>> compensationMono = createCreditMono(customerId, amount, tuitionId);

        try {
            compensationMono.block(); 
            System.err.println("[COMPENSATION SUCCESS] Customer balance credited back for Tuition ID: " + tuitionId + ". System state restored.");
        } catch (Exception compensationError) {
            System.err.println("CRITICAL ERROR: Compensation (Credit) failed for Customer ID " + customerId + ". State is now INCONSISTENT! MANUAL INTERVENTION REQUIRED!");
            throw new RuntimeException("Compensation (Credit) failed, System state inconsistent. Manual check required for Customer ID: " + customerId + ". " + compensationError.getMessage(), compensationError);
        }
    }

    private Mono<GenericResponse<Object>> createGetTuitionMono(Long customerId, Long tuitionId) {
        return webClient.get()
            .uri(TUITION_SERVICE_BASE_URL + "/id/" + tuitionId)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header("X-Customer-Id", String.valueOf(customerId))
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> 
                clientResponse.bodyToMono(new ParameterizedTypeReference<GenericResponse<Object>>() {})
                    .flatMap(response -> {
                        String message = response.getMessage() != null ? response.getMessage() : "Unknown error";
                        return Mono.error(new RuntimeException("Tuition Fetch Failed: " + message));
                    })
            )
            .bodyToMono(new ParameterizedTypeReference<GenericResponse<Object>>() {});
    }

    private Mono<GenericResponse<Integer>> createDebitMono(Long customerId, PaymentRequest request) {
        Map<String, Object> debitRequest = Map.of(
            "amount", request.getAmount(),
            "description", "Tuition Payment for ID " + request.getTuitionId()
        );

        return webClient.post()
            .uri(CUSTOMER_SERVICE_BASE_URL + "/balance/debit")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header("X-Customer-Id", String.valueOf(customerId))
            .bodyValue(debitRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> 
                clientResponse.bodyToMono(new ParameterizedTypeReference<GenericResponse<Object>>() {})
                    .flatMap(response -> Mono.error(new RuntimeException("Customer Debit Failed: " + response.getMessage())))
            )
            .bodyToMono(new ParameterizedTypeReference<GenericResponse<Integer>>() {});
    }

    private Mono<GenericResponse<Integer>> createCreditMono(Long customerId, Integer amount, Long tuitionId) {
         Map<String, Object> creditRequest = Map.of(
            "amount", amount,
            "description", "Compensation Credit for failed Tuition ID: " + tuitionId
        );

        return webClient.post()
            .uri(CUSTOMER_SERVICE_BASE_URL + "/balance/credit")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header("X-Customer-Id", String.valueOf(customerId))
            .bodyValue(creditRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> 
                clientResponse.bodyToMono(new ParameterizedTypeReference<GenericResponse<Object>>() {})
                    .flatMap(response -> Mono.error(new RuntimeException("Compensation Credit Failed: " + response.getMessage())))
            )
            .bodyToMono(new ParameterizedTypeReference<GenericResponse<Integer>>() {});
    }
    
    private Mono<GenericResponse<Object>> createTuitionUpdateMono(Long customerId, PaymentRequest request, boolean isPaid) {
        Map<String, Object> tuitionUpdateRequest = Map.of(
            "tuitionId", request.getTuitionId(),
            "studentId", request.getStudentId(),
            "isPaid", isPaid
        );
        
        return webClient.post()
            .uri(TUITION_SERVICE_BASE_URL + "/status")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header("X-Customer-Id", String.valueOf(customerId))
            .bodyValue(tuitionUpdateRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> 
                clientResponse.bodyToMono(new ParameterizedTypeReference<GenericResponse<Object>>() {})
                    .flatMap(response -> Mono.error(new RuntimeException("Tuition Update Failed: " + response.getMessage())))
            )
            .bodyToMono(new ParameterizedTypeReference<GenericResponse<Object>>() {});
    }
    
    private RuntimeException mapToPaymentFailure(String step, Exception e) {
        String rootMessage = e.getMessage();
        if (rootMessage != null && rootMessage.contains(step + ": ")) {
            return new IllegalArgumentException(rootMessage.substring(rootMessage.indexOf(": ") + 2), e);
        }
        return new RuntimeException("Payment processing failed due to internal error during [" + step + "]: " + e.getMessage(), e);
    }
}