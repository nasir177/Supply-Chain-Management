package com.sorascm.backend.catalog.repository;

import com.sorascm.backend.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface CategoryRepository extends JpaRepository<Category, Long>{
    Optional<Category> findByCode(String code);
    boolean existsByCode(String code);
}
