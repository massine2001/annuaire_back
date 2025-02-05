package com.example.demo.services;

import com.example.demo.models.Pool;
import com.example.demo.repositories.PoolRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PoolService {
    private final PoolRepository poolRepository;
    public PoolService(PoolRepository poolRepository) {
        this.poolRepository = poolRepository;
    }

    public List<Pool> getAllPools() {
        return poolRepository.findAll();
    }

    public Pool getPoolById(int id) {
        return poolRepository.findById(id);
    }
    public Pool savePool(Pool pool) {
        return poolRepository.save(pool);
    }
    public void deletePoolById(int id) {
        poolRepository.deleteById(id);
    }
    public Pool updatePool(int id,Pool pool) {
        Pool modifiedPool = poolRepository.findById(id);
        modifiedPool.setName(pool.getName());
        modifiedPool.setDescription(pool.getDescription());
        return poolRepository.save(modifiedPool);
    }
}
