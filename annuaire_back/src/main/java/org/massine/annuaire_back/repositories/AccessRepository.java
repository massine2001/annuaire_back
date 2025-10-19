package org.massine.annuaire_back.repositories;

import com.example.demo.models.Access;
import com.example.demo.models.Pool;
import com.example.demo.models.User;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessRepository extends JpaRepository<Access, Integer> {
    @Nonnull
    List<Access> findAll();
    Optional<Access> findById(int id);
    @Query("SELECT u FROM User u JOIN Access a ON u.id = a.user.id WHERE a.pool.id = :pool_id")
    List<User> getUsersFromPool(@Param("pool_id") int pool_id);
    @Query("SELECT count(u) FROM User u JOIN Access a ON u.id = a.user.id WHERE a.pool.id = :pool_id")
    int getCountUsersFromPool(@Param("pool_id") int pool_id);

    @Query("SELECT p FROM Pool p JOIN Access a ON p.id = a.pool.id WHERE a.user.id = :user_id")
    List<Pool> getPoolsFromUser(@Param("user_id") int user_id);
    @Query("SELECT count(p) FROM Pool p JOIN Access a ON p.id = a.pool.id WHERE a.user.id = :user_id")
    int getCountPoolsFromUser(@Param("user_id") int user_id);
    boolean existsByUserIdAndPoolId(int userId, int poolId);


    Optional<Access> findByUserIdAndPoolId(int userId, int poolId);


    List<Access> findByUserId(int userId);


    List<Access> findByPoolId(int poolId);
}
