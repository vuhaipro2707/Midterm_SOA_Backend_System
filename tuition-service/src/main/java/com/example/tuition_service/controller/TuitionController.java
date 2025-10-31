package com.example.tuition_service.controller;

import com.example.tuition_service.dto.GenericResponse;
import com.example.tuition_service.dto.TuitionPaymentRequest;
import com.example.tuition_service.model.StudentTuition;
import com.example.tuition_service.service.TuitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;

import java.util.List;

@RestController
public class TuitionController {

    @Autowired
    private TuitionService tuitionService;

    @GetMapping("/studentId/{studentId}")
    private ResponseEntity<GenericResponse<List<StudentTuition>>> getTuitions(@PathVariable String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
             return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid Student ID provided."));
        }
        
        try {
            List<StudentTuition> tuitions = tuitionService.getTuitionsByStudentId(studentId);

            if (tuitions.isEmpty()) {
                return ResponseEntity.status(404).body(GenericResponse.failure("No tuition records found for student ID: " + studentId));
            } else {
                return ResponseEntity.ok(GenericResponse.success("Tuition records retrieved successfully for student ID: " + studentId, tuitions));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/status")
    public ResponseEntity<GenericResponse<StudentTuition>> updateTuitionStatus(@RequestBody TuitionPaymentRequest request) {
        if (request.getTuitionId() == null || request.getIsPaid() == null || request.getStudentId() == null) {
             return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid request: tuitionId, isPaid status, and studentId are required."));
        }
        
        try {
            Optional<StudentTuition> updatedTuitionOpt = tuitionService.updateTuitionStatus(
                request.getTuitionId(),
                request.getStudentId(),
                request.getIsPaid()
            );
        
            if (updatedTuitionOpt.isPresent()) {
                StudentTuition updatedTuition = updatedTuitionOpt.get();
                return ResponseEntity.ok(GenericResponse.success(
                    "Tuition payment status updated successfully.", 
                    updatedTuition
                ));
            } else {
                return ResponseEntity.status(404).body(GenericResponse.failure("Tuition record not found with ID: " + request.getTuitionId()));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/id/{tuitionId}")
    public ResponseEntity<GenericResponse<StudentTuition>> getTuitionById(@PathVariable Long tuitionId) {
        if (tuitionId == null) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid Tuition ID provided."));
        }
        
        try {
            Optional<StudentTuition> tuitionOpt = tuitionService.getTuitionById(tuitionId);

            if (tuitionOpt.isPresent()) {
                return ResponseEntity.ok(GenericResponse.success("Tuition record retrieved successfully.", tuitionOpt.get()));
            } else {
                return ResponseEntity.status(404).body(GenericResponse.failure("Tuition record not found with ID: " + tuitionId));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error: " + e.getMessage()));
        }
    }
}