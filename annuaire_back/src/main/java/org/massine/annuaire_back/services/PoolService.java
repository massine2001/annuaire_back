package com.example.demo.services;

import com.example.demo.models.Pool;
import com.example.demo.models.Access;
import com.example.demo.repositories.PoolRepository;
import com.example.demo.repositories.AccessRepository;
import com.example.demo.repositories.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PoolService {
    private final PoolRepository poolRepository;
    private final AccessRepository accessRepository;
    private final FileRepository fileRepository;

    public PoolService(PoolRepository poolRepository, AccessRepository accessRepository, FileRepository fileRepository) {
        this.poolRepository = poolRepository;
        this.accessRepository = accessRepository;
        this.fileRepository = fileRepository;
    }

    public List<Pool> getAllPools() {
        return poolRepository.findAll();
    }

    public long getPoolsCount() {
        return poolRepository.count();
    }

    public Pool getPoolById(int id) {
        return poolRepository.findById(id);
    }
    public Pool savePool(Pool pool) {
        return poolRepository.save(pool);
    }

    @Transactional
    public void deletePoolById(int id) {
        accessRepository.findByPoolId(id).forEach(access -> accessRepository.delete(access));

        fileRepository.findByPoolId(id).forEach(file -> fileRepository.delete(file));

        poolRepository.deleteById(id);
    }
    public Pool updatePool(int id,Pool pool) {
        Pool modifiedPool = poolRepository.findById(id);
        modifiedPool.setName(pool.getName());
        modifiedPool.setDescription(pool.getDescription());
        return poolRepository.save(modifiedPool);
    }

    public List<Pool> getAllPoolsByUserId(int userId) {
        List<Access> accesses = accessRepository.findByUserId(userId);

        List<Pool> pools = accesses.stream()
                .map(Access::getPool)
                .collect(Collectors.toList());

        return pools;
    }
}