package com.example.tuition_service.repository;

import com.example.tuition_service.model.StudentTuition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentTuitionRepository extends JpaRepository<StudentTuition, Long> {
    List<StudentTuition> findByStudentId(String studentId);
}