package com.example.demo.repositories;

import com.example.demo.models.Pool;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoolRepository extends JpaRepository<Pool, Integer> {
    @Nonnull
    List<Pool> findAll();
    Pool findById(int id);

}
