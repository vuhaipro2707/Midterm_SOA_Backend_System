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
    public Optional<StudentTuition> updateTuitionStatus(Long tuitionId, Boolean isPaid) {
        Optional<StudentTuition> tuitionOpt = studentTuitionRepository.findById(tuitionId);

        if (tuitionOpt.isEmpty()) {
            return Optional.empty();
        }

        StudentTuition tuition = tuitionOpt.get();
        
        tuition.setIsPaid(isPaid);
        studentTuitionRepository.save(tuition);

        return Optional.of(tuition);
    }
}