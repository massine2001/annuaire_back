package org.massine.annuaire_back.controllers;

import org.massine.annuaire_back.models.Access;
import org.massine.annuaire_back.models.User;
import org.massine.annuaire_back.services.AccessService;
import org.massine.annuaire_back.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/access/")
public class AccessController {
    private final AccessService accessService;
    private final UserService userService;

    public AccessController(AccessService accessService, UserService userService) {
        this.accessService = accessService;
        this.userService = userService;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof String)) {
            return null;
        }
        String email = (String) authentication.getPrincipal();
        return userService.findByEmailSafe(email);
    }


    @GetMapping("/")
    public ResponseEntity<List<Access>> getAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Access> accesses = accessService.getAllAccess();
        if (accesses.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(accesses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Access> getAccessById(@PathVariable int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<Access> accessOpt = accessService.getAccessById(id);

        if (accessOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Access access = accessOpt.get();

        boolean isOwner = access.getUser().getId().equals(currentUser.getId());
        boolean isPoolAdmin = accessService.userIsOwnerOrAdmin(currentUser.getId(), access.getPool().getId());
        boolean isSystemAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isPoolAdmin && !isSystemAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(access);
    }


    @PostMapping("/")
    public ResponseEntity<Access> createAccess(@RequestBody Access access) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (access.getPool() == null) {
            return ResponseEntity.badRequest().build();
        }

        if (!accessService.userIsOwnerOrAdmin(currentUser.getId(), access.getPool().getId())
            && !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if ("owner".equalsIgnoreCase(access.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null); 
        }

        Access createdAccess = accessService.saveAccess(access);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccess);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Access> updateAccess(@PathVariable int id, @RequestBody Access access) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<Access> existingAccessOpt = accessService.getAccessById(id);

        if (existingAccessOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Access existingAccess = existingAccessOpt.get();

        if (!accessService.userIsOwnerOrAdmin(currentUser.getId(), existingAccess.getPool().getId())
            && !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if ("owner".equalsIgnoreCase(existingAccess.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (access.getRole() != null && "owner".equalsIgnoreCase(access.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Access modifiedAccess = accessService.upadateAccess(id, access);
        return ResponseEntity.ok(modifiedAccess);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccess(@PathVariable int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<Access> accessOpt = accessService.getAccessById(id);

        if (accessOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Access access = accessOpt.get();

        if ("owner".equalsIgnoreCase(access.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean isSelf = access.getUser().getId().equals(currentUser.getId());
        boolean isPoolAdmin = accessService.userIsOwnerOrAdmin(currentUser.getId(), access.getPool().getId());
        boolean isSystemAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (!isSelf && !isPoolAdmin && !isSystemAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        accessService.deleteAccess(id);
        return ResponseEntity.noContent().build();
    }
}

