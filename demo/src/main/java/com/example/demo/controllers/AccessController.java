package com.example.demo.controllers;

import com.example.demo.models.Access;
import com.example.demo.models.User;
import com.example.demo.services.AccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/access/")
public class AccessController {
    private final AccessService accessService;
    public AccessController(AccessService accessService) {
        this.accessService = accessService;
    }
    @GetMapping("/")
    public ResponseEntity<List<Access>> getAccess() {
        List<Access> accesses = accessService.getAllAccess();
        if (accesses.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(accesses);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Optional<Access>> getAccessById(@PathVariable int id) {
        Optional<Access> access = accessService.getAccessById(id);
        if (access.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(access);
    }
    @PostMapping("/")
    public ResponseEntity<Access> createAccess(@RequestBody Access access) {
        Access access1 = accessService.saveAccess(access);
        if (access1 == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(access1);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Access> updateAccess(@PathVariable int id, @RequestBody Access access) {
        Access modifiedAccess = accessService.upadateAccess(id, access);
        return ResponseEntity.ok(modifiedAccess);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Access> deleteAccess(@PathVariable int id) {
        if (accessService.getAccessById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        accessService.deleteAccess(id);
        return ResponseEntity.noContent().build();
    }

}
