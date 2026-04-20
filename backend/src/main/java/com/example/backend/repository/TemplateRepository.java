package com.example.backend.repository;

import com.example.backend.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findByUserEmail(String email);
    boolean existsByNameAndUserEmail(String name, String email);
}