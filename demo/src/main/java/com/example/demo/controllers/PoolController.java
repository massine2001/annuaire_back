package com.example.demo.controllers;

import com.example.demo.models.Access;
import com.example.demo.models.File;
import com.example.demo.models.Pool;
import com.example.demo.models.User;
import com.example.demo.services.AccessService;
import com.example.demo.services.FileService;
import com.example.demo.services.PoolService;
import com.example.demo.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pool/")
public class PoolController {
    private final PoolService poolService;
    private final AccessService accessService;
    private final FileService fileService;
    private final UserService userService;

    public PoolController(PoolService poolService, AccessService accessService, FileService fileService, UserService userService) {
        this.poolService = poolService;
        this.accessService = accessService;
        this.fileService = fileService;
        this.userService = userService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Pool>> getPools() {
        List<Pool> pools = poolService.getAllPools();
        if(pools.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(pools);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getPoolsCount() {
        long count = poolService.getPoolsCount();
        return ResponseEntity.ok(count);
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

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Pool>> getAllPoolsByUserId(@PathVariable int userId) {
        List<Pool> pools = poolService.getAllPoolsByUserId(userId);
        if (pools.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(pools);
    }
    @GetMapping("/stats/{poolId}")
    public ResponseEntity<Map<String, Object>> getPoolStats(@PathVariable int poolId) {
        Pool pool = poolService.getPoolById(poolId);
        if (pool == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> stats = new HashMap<>();

        // ==================== INFOS DE BASE ====================
        stats.put("pool", pool);

        // ==================== STATS MEMBRES ====================
        List<User> members = accessService.getUsersFromPool(poolId);
        List<Access> accesses = accessService.getAccessesByPool(poolId); // À créer

        stats.put("membersCount", members.size());
        stats.put("members", members);
        stats.put("accesses", accesses);

// Répartition par rôle dans la pool
        Map<String, Long> roleDistribution = accesses.stream()
                .filter(access -> access.getRole() != null)
                .collect(Collectors.groupingBy(Access::getRole, Collectors.counting()));
        stats.put("roleDistribution", roleDistribution);

// Répartition par rôle global des users
        Map<String, Long> userRoleDistribution = members.stream()
                .filter(user -> user.getRole() != null)
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
        stats.put("userRoleDistribution", userRoleDistribution);

        // ==================== STATS FICHIERS ====================
        List<File> files = fileService.findByPoolId(poolId); // À créer

        stats.put("filesCount", files.size());
        stats.put("files", files);

        // Taille totale (à calculer si tu stockes la taille dans File)
        // long totalSize = files.stream().mapToLong(File::getSize).sum();
        // stats.put("totalSize", totalSize);

        // Top uploaders
// Top uploaders
        Map<User, Long> uploaderStats = files.stream()
                .filter(file -> file.getUserUploader() != null)
                .collect(Collectors.groupingBy(File::getUserUploader, Collectors.counting()));
        List<Map.Entry<User, Long>> topUploaders = uploaderStats.entrySet().stream()
                .sorted(Map.Entry.<User, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());
        stats.put("topUploaders", topUploaders);
  // Fichiers par jour (7 derniers jours)
        Map<String, Long> filesPerDay = files.stream()
                .collect(Collectors.groupingBy(
                        file -> file.getCreatedAt().toString().substring(0, 10),
                        Collectors.counting()
                ));
        stats.put("filesPerDay", filesPerDay);

        // Dernier fichier uploadé
        Optional<File> lastFile = files.stream()
                .max(Comparator.comparing(File::getCreatedAt));
        stats.put("lastFile", lastFile.orElse(null));

        // ==================== STATS ACTIVITÉ ====================
        // Membres les plus actifs (par nombre de fichiers)
        stats.put("mostActiveMembers", topUploaders);

        // Membres inactifs (0 fichiers uploadés)
        List<User> inactiveMembers = members.stream()
                .filter(user -> files.stream().noneMatch(f -> f.getUserUploader().getId().equals(user.getId())))
                .collect(Collectors.toList());
        stats.put("inactiveMembers", inactiveMembers);
        stats.put("inactiveMembersCount", inactiveMembers.size());

        // ==================== STATS TEMPORELLES ====================
        // Date de création de la pool
        stats.put("poolCreatedAt", pool.getCreatedAt());

        // Âge de la pool en jours
        long poolAgeInDays = ChronoUnit.DAYS.between(
                pool.getCreatedAt(),
                Instant.now()
        );
        stats.put("poolAgeInDays", poolAgeInDays);

        // Nouveau membre le plus récent
        Optional<User> newestMember = members.stream()
                .max(Comparator.comparing(User::getCreatedAt));
        stats.put("newestMember", newestMember.orElse(null));

        // Membre le plus ancien
        Optional<User> oldestMember = members.stream()
                .min(Comparator.comparing(User::getCreatedAt));
        stats.put("oldestMember", oldestMember.orElse(null));

        // ==================== STATS AVANCÉES ====================
        // Moyenne de fichiers par membre
        double avgFilesPerMember = members.isEmpty() ? 0 : (double) files.size() / members.size();
        stats.put("avgFilesPerMember", Math.round(avgFilesPerMember * 100.0) / 100.0);

        // Taux d'activité (% de membres ayant uploadé au moins 1 fichier)
        double activityRate = members.isEmpty() ? 0 :
                ((double) (members.size() - inactiveMembers.size()) / members.size() * 100);
        stats.put("activityRate", Math.round(activityRate * 100.0) / 100.0);

        // Extensions de fichiers (à partir du nom)
        Map<String, Long> fileExtensions = files.stream()
                .map(f -> {
                    String name = f.getName();
                    int lastDot = name.lastIndexOf('.');
                    return lastDot > 0 ? name.substring(lastDot + 1).toLowerCase() : "sans extension";
                })
                .collect(Collectors.groupingBy(ext -> ext, Collectors.counting()));
        stats.put("fileExtensions", fileExtensions);

        // Créateur de la pool
        if (pool.getCreatedBy() != null) {
            User creator = userService.findById(pool.getCreatedBy());
            stats.put("creator", creator);
        }

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/files/{poolId}")
    public ResponseEntity<List<File>> getFilesOfPool(@PathVariable int poolId) {
        // Vérifier si la pool existe
        Pool pool = poolService.getPoolById(poolId);
        if (pool == null) {
            return ResponseEntity.notFound().build();
        }

        List<File> files = fileService.findByPoolId(poolId);
        if (files.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(files);
    }

    @GetMapping("/files/count/{poolId}")
    public ResponseEntity<Long> getFilesCountOfPool(@PathVariable int poolId) {
        Pool pool = poolService.getPoolById(poolId);
        if (pool == null) {
            return ResponseEntity.notFound().build();
        }

        long count = fileService.findByPoolId(poolId).size();
        return ResponseEntity.ok(count);
    }

}
