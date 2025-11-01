package com.example.payment_processor_service.service;

import com.example.payment_processor_service.dto.GenericResponse;
import com.example.payment_processor_service.dto.PaymentConfirmationRequest;
import com.example.payment_processor_service.dto.PaymentInitiateRequest;
import com.example.payment_processor_service.model.PaymentTransaction;
import com.example.payment_processor_service.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private WebClient webClient;

    private record OtpServiceResponse(String otpCode, String statusMessage) {}
    
    private static final String CUSTOMER_SERVICE_BASE_URL = "http://customer-management-service:8082";
    private static final String TUITION_SERVICE_BASE_URL = "http://tuition-service:8084";
    private static final String OTP_SERVICE_BASE_URL = "http://otp-service:8085";

    @Transactional(readOnly = true)
    public List<PaymentTransaction> getPaymentHistory(Long customerId) {
        return paymentTransactionRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public void initiatePayment(Long customerId, PaymentInitiateRequest request) {
        if (paymentTransactionRepository.findByTuitionId(request.getTuitionId()).isPresent()) {
            throw new IllegalArgumentException("Tuition ID " + request.getTuitionId() + " has already been paid.");
        }

        OtpServiceResponse otpResult = getOtpFromService(customerId, request.getTuitionId(), "/generate");
        String otpCode = otpResult.otpCode();

        if (!otpResult.statusMessage().contains("Existing")) {
            String customerEmail = getCustomerEmail(customerId);
            //sendOtpEmail(customerEmail, otpCode, request.getTuitionId(), request.getAmount());
            System.out.println("OTP Code for Tuition ID " + request.getTuitionId() + " sent to email: " + customerEmail + " is: " + otpCode);
        } else {
             System.out.println("[INFO] Existing OTP reused for Tuition ID " + request.getTuitionId() + ". Skipping email dispatch.");
        }
    }


    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public PaymentTransaction confirmPayment(Long customerId, PaymentConfirmationRequest request) {
        Mono<GenericResponse<Object>> otpValidateMono = createOtpValidationMono(customerId, request.getTuitionId(), request.getOtpCode());
        try {
            otpValidateMono.block();
        } catch (Exception e) {
            throw mapToPaymentFailure("OTP Validation Failed", e);
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

        if (requiredAmount == null) {
            throw new IllegalArgumentException(
                "Missing amount in tuition record"
            );
        }
        
        // 3. Thực hiện Debit
        Mono<GenericResponse<Integer>> debitMono = createDebitMono(customerId, request.getTuitionId(), requiredAmount); 
        try {
            debitMono.block(); 
        } catch (Exception e) {
            throw mapToPaymentFailure("Customer Debit Failed", e);
        }

        // 4. Update Tuition
        Mono<GenericResponse<Object>> tuitionUpdateMono = createTuitionUpdateMono(customerId, request, true);
        try {
            tuitionUpdateMono.block(); 
        } catch (Exception e) {
            compensateCustomerDebit(customerId, requiredAmount, request.getTuitionId());
            throw mapToPaymentFailure("Tuition Update Failed", e);
        }
        
        // 5. Record Transaction
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setCustomerId(customerId);
        transaction.setTuitionId(request.getTuitionId());
        transaction.setAmount(requiredAmount);
        transaction.setPaymentDate(ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime());
        
        return paymentTransactionRepository.save(transaction);
    }
    

    @Transactional(readOnly = true)
    public void resendOtp(Long customerId, PaymentInitiateRequest request) {
        if (paymentTransactionRepository.findByTuitionId(request.getTuitionId()).isPresent()) {
            throw new IllegalArgumentException("Tuition ID " + request.getTuitionId() + " has already been paid. Resend is unnecessary.");
        }

        // Gọi trực tiếp đến endpoint /resend của OTP Service để buộc tạo mã mới
        OtpServiceResponse otpResult = getOtpFromService(customerId, request.getTuitionId(), "/resend"); 

        String customerEmail = getCustomerEmail(customerId);
        System.out.println("[INFO] Forced RESEND. Old OTP deleted. New OTP for Tuition ID " + request.getTuitionId() + " sent to email: " + customerEmail + " is: " + otpResult.otpCode());
    }

    private OtpServiceResponse getOtpFromService(Long customerId, Long tuitionId, String endpoint) {
        Map<String, Object> otpRequest = Map.of("tuitionId", tuitionId);

        try {
            GenericResponse<Map<String, String>> otpResponse = webClient.post()
                .uri(OTP_SERVICE_BASE_URL + endpoint)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("X-Customer-Id", String.valueOf(customerId))
                .bodyValue(otpRequest)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<GenericResponse<Map<String, String>>>() {})
                .block(); 
                
            if (otpResponse != null && otpResponse.isSuccess() && otpResponse.getData() != null && otpResponse.getData().containsKey("otpCode")) {
                String otpCode = otpResponse.getData().get("otpCode");
                String statusMessage = otpResponse.getData().getOrDefault("statusMessage", "OTP generated."); 
                return new OtpServiceResponse(otpCode, statusMessage); 
            }
            String message = otpResponse != null && otpResponse.getMessage() != null ? 
                             otpResponse.getMessage() : 
                             "Unknown OTP generation error.";
            throw new RuntimeException(message);
        } catch (Exception e) {
             throw mapToPaymentFailure("OTP Generation Failed", e);
        }
    }
    
    private Mono<GenericResponse<Object>> createOtpValidationMono(Long customerId, Long tuitionId, String otpCode) {
        Map<String, Object> validateRequest = Map.of(
            "tuitionId", tuitionId,
            "otpCode", otpCode
        );

        return webClient.post()
            .uri(OTP_SERVICE_BASE_URL + "/validate")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header("X-Customer-Id", String.valueOf(customerId))
            .bodyValue(validateRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> 
                clientResponse.bodyToMono(new ParameterizedTypeReference<GenericResponse<Object>>() {})
                    .flatMap(response -> Mono.error(new RuntimeException("OTP Validation Failed: Invalid or expired OTP.")))
            )
            .bodyToMono(new ParameterizedTypeReference<GenericResponse<Object>>() {});
    }

    private String getCustomerEmail(Long customerId) {
        try {
            GenericResponse<Map<String, Object>> customerResponse = webClient.get()
                .uri(CUSTOMER_SERVICE_BASE_URL + "/info")
                .header("X-Customer-Id", String.valueOf(customerId)) 
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<GenericResponse<Map<String, Object>>>() {})
                .block();
                
            if (customerResponse != null && customerResponse.isSuccess() && customerResponse.getData() != null && customerResponse.getData().containsKey("email")) {
                return customerResponse.getData().get("email").toString();
            }
            throw new RuntimeException("Customer email not found.");
        } catch (Exception e) {
             throw new RuntimeException("Failed to fetch customer email: " + e.getMessage(), e);
        }
    }

    // private void sendOtpEmail(String email, String otpCode, Long tuitionId, Integer amount) {
    //     // [MOCK] Mô phỏng gọi Mail Service.
    //     System.out.println("--- [MAIL SERVICE MOCK] ---");
    //     System.out.println("TO: " + email);
    //     System.out.println("SUBJECT: Your OTP for Tuition Payment");
    //     System.out.println("BODY: Your One-Time Password for payment (Tuition ID: " + tuitionId + ", Amount: " + amount + ") is: " + otpCode);
    //     System.out.println("-------------------------");
    // }
    
    private void compensateCustomerDebit(Long customerId, Integer amount, Long tuitionId) {
        System.err.println("[COMPENSATION] Tuition Update failed. Attempting to CREDIT back amount " + amount + " for customer ID: " + customerId);

        Mono<GenericResponse<Integer>> compensationMono = createCreditMono(customerId, tuitionId, amount);

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

    private Mono<GenericResponse<Integer>> createDebitMono(Long customerId, Long tuitionId, Integer amount) {
        Map<String, Object> debitRequest = Map.of(
            "amount", amount,
            "description", "Tuition Payment for ID " + tuitionId
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

    private Mono<GenericResponse<Integer>> createCreditMono(Long customerId, Long tuitionId, Integer amount) {
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

    private Mono<GenericResponse<Object>> createTuitionUpdateMono(Long customerId, PaymentConfirmationRequest request, boolean isPaid) {
        Map<String, Object> tuitionUpdateRequest = Map.of(
            "tuitionId", request.getTuitionId(),
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