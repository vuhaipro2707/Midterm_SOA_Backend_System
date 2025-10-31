package com.example.tuition_service.service;

import com.example.tuition_service.model.StudentTuition;
import com.example.tuition_service.repository.StudentTuitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TuitionService {

    @Autowired
    private StudentTuitionRepository studentTuitionRepository;

    public List<StudentTuition> getTuitionsByStudentId(String studentId) {
        return studentTuitionRepository.findByStudentId(studentId);
    }

    public Optional<StudentTuition> getTuitionById(Long tuitionId) {
        return studentTuitionRepository.findById(tuitionId);
    }

    @Transactional
    public Optional<StudentTuition> updateTuitionStatus(Long tuitionId, String studentId, Boolean isPaid) {
        Optional<StudentTuition> tuitionOpt = studentTuitionRepository.findById(tuitionId);

        if (tuitionOpt.isEmpty()) {
            return Optional.empty();
        }

        StudentTuition tuition = tuitionOpt.get();

        if (!tuition.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Tuition record with ID " + tuitionId + " does not belong to student ID " + studentId + ".");
        }
        
        tuition.setIsPaid(isPaid);
        studentTuitionRepository.save(tuition);

        return Optional.of(tuition);
    }
}