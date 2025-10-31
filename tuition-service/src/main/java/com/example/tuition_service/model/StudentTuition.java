package com.example.tuition_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "student_tuitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentTuition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tuitionId;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private String semester;

    @Column(nullable = false)
    private String academicYear;

    @Column(nullable = false)
    private Boolean isPaid = false;
}