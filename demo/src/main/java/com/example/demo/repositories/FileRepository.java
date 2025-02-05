package com.example.demo.repositories;

import com.example.demo.models.File;
import com.example.demo.models.Pool;
import com.example.demo.models.User;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Integer> {
    @Nonnull
    List<File> findAll();
    File findById(int id);
    @Query("SELECT p FROM Pool p JOIN File f ON p.id = f.pool.id WHERE f.id = :id")
    Pool findPoolById(@Param("id")int id);
    @Query("SELECT path FROM File WHERE id = :id")
    String findPath(@Param("id")int id);
    @Query("SELECT u FROM User u JOIN File f ON u.id = f.userUploader.id WHERE f.id = :id")
    User findUploader(int id);

}
