package org.massine.annuaire_back.repositories;

import com.example.demo.models.File;
import com.example.demo.models.Pool;
import com.example.demo.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Integer> {

    @Override
    List<File> findAll();

    @Override
    Optional<File> findById(Integer id);

    @Query("SELECT p FROM Pool p JOIN File f ON p.id = f.pool.id WHERE f.id = :id")
    Pool findPoolById(@Param("id") int id);

    @Query("SELECT f.path FROM File f WHERE f.id = :id")
    String findPath(@Param("id") int id);

    @Query("SELECT u FROM User u JOIN File f ON u.id = f.userUploader.id WHERE f.id = :id")
    User findUploader(@Param("id") int id);

    List<File> findByPoolId(int poolId);
}
