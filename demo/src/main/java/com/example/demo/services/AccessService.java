package com.example.demo.services;

import com.example.demo.models.Access;
import com.example.demo.models.Pool;
import com.example.demo.models.User;
import com.example.demo.repositories.AccessRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccessService {
    private final AccessRepository accessRepository;
    public AccessService(AccessRepository accessRepository) {
        this.accessRepository = accessRepository;
    }
    public List<Access> getAllAccess() {
        return accessRepository.findAll();
    }
    public List<User> getUsersFromPool(int pool_id){
        return accessRepository.getUsersFromPool(pool_id);
    }
    public List<Pool> getPoolsFromUser(int user_id){
        return accessRepository.getPoolsFromUser(user_id);
    }
    public int countPoolsByUser(int user_id){
        return accessRepository.getCountPoolsFromUser(user_id);
    }
    public int countUsersByPool(int pool_id){
        return accessRepository.getCountUsersFromPool(pool_id);
    }
    public Access saveAccess(Access access){
        return accessRepository.save(access);
    }
    public Optional<Access> getAccessById(int access_id){
        return accessRepository.findById(access_id);
    }
    public Access upadateAccess(int id,Access access){
        Access accessUpdated = accessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User with id : "+id+" not found"));
        if(access.getRole() != null){
            accessUpdated.setRole(access.getRole());
        }
        if(access.getPool() != null){
            accessUpdated.setPool(access.getPool());
        }
        if(access.getUser() != null){
            accessUpdated.setUser(access.getUser());
        }
        return accessRepository.save(accessUpdated);
    }
    public void deleteAccess(int access_id){
        accessRepository.deleteById(access_id);
    }

}
