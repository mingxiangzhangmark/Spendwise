package com.example.backend.repository;

import com.example.backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Category findBycategoryName(String categoryName);
}