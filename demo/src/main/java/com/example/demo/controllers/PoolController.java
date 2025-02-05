package com.example.demo.controllers;

import com.example.demo.models.Pool;
import com.example.demo.models.User;
import com.example.demo.services.AccessService;
import com.example.demo.services.PoolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pool/")
public class PoolController {
    private final PoolService poolService;
    private final AccessService accessService;

    public PoolController(PoolService poolService, AccessService accessService) {
        this.poolService = poolService;
        this.accessService = accessService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Pool>> getPools() {
        List<Pool> pools = poolService.getAllPools();
        if(pools.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(pools);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Pool> getPoolById(@PathVariable("id") int id) {
        Pool pool = poolService.getPoolById(id);
        if(pool == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pool);
    }

    @PostMapping("/")
    public ResponseEntity<Pool> createPool(@RequestBody Pool pool) {
        Pool createdPool = poolService.savePool(pool);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPool);
    }

    @PutMapping("/id")
    public ResponseEntity<Pool> updatePool(@RequestBody Pool pool, int id) {
        Pool updatedPool = poolService.updatePool(id, pool);
        return ResponseEntity.ok(updatedPool);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Pool> deletePool(@PathVariable("id") int id) {
        if (poolService.getPoolById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        poolService.deletePoolById(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/users/{id}")
    public ResponseEntity<List<User>> getUsersFromPool(@PathVariable int id){
        List<User> users = accessService.getUsersFromPool(id);
        if(users.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(users);
    }
    @GetMapping("/users/count/{id}")
    public int countUsersByPool(@PathVariable int id){
        return accessService.countUsersByPool(id);
    }
}
