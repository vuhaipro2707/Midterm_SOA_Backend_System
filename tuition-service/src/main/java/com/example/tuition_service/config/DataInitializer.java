package com.example.tuition_service.config;

import com.example.tuition_service.model.StudentTuition;
import com.example.tuition_service.repository.StudentTuitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Autowired
    StudentTuitionRepository studentTuitionRepository;

    @Bean
    public CommandLineRunner initializeTuitionData() {
        return args -> {
            if (studentTuitionRepository.count() == 0) {
                
                List<StudentTuition> initialTuitions = Arrays.asList(
                    new StudentTuition(null, "523H1101", "Nguyễn Văn A", 2800000, "I", "2023-2024", false),
                    new StudentTuition(null, "523H1101", "Nguyễn Văn A", 2100000, "II", "2023-2024", false),
                    new StudentTuition(null, "523H1103", "Trần Thị B", 4500000, "II", "2024-2025", false),
                    new StudentTuition(null, "523H1104", "Phạm Văn C", 1500000, "I", "2023-2024", false)
                );

                studentTuitionRepository.saveAll(initialTuitions);
                
                System.out.println("--- Successfully created " + initialTuitions.size() + " test tuition records in the database. ---");
            }
        };
    }
}