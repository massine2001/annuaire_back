package com.example.demo.controllers;

import com.example.demo.models.File;
import com.example.demo.models.Pool;
import com.example.demo.services.FileService;
import com.example.demo.services.PoolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PublicController {

    private final PoolService poolService;
    private final FileService fileService;

    public PublicController(PoolService poolService, FileService fileService) {
        this.poolService = poolService;
        this.fileService = fileService;
    }

    /**
     * Récupérer la liste des pools publics (pour les visiteurs non connectés)
     */
    @GetMapping("/pools/public")
    public ResponseEntity<List<Map<String, Object>>> getPublicPools() {
        List<Pool> publicPools = poolService.getAllPools().stream()
                .filter(pool -> pool.getPublicAccess() != null && pool.getPublicAccess())
                .collect(Collectors.toList());

        List<Map<String, Object>> poolsWithFileCount = publicPools.stream()
                .map(pool -> {
                    Map<String, Object> poolData = Map.of(
                            "id", pool.getId(),
                            "name", pool.getName(),
                            "description", pool.getDescription() != null ? pool.getDescription() : "",
                            "createdAt", pool.getCreatedAt(),
                            "fileCount", fileService.getFilesByPoolId(pool.getId()).size(),
                            "isPublic", true
                    );
                    return poolData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(poolsWithFileCount);
    }

    /**
     * Récupérer les détails d'un pool public spécifique
     */
    @GetMapping("/pools/{poolId}/public")
    public ResponseEntity<?> getPublicPoolDetails(@PathVariable int poolId) {
        Pool pool = poolService.getPoolById(poolId);

        if (pool == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Pool non trouvé"));
        }

        if (pool.getPublicAccess() == null || !pool.getPublicAccess()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Ce pool n'est pas public"));
        }

        Map<String, Object> poolDetails = Map.of(
                "id", pool.getId(),
                "name", pool.getName(),
                "description", pool.getDescription() != null ? pool.getDescription() : "",
                "createdAt", pool.getCreatedAt(),
                "isPublic", true
        );

        return ResponseEntity.ok(poolDetails);
    }

    /**
     * Récupérer les fichiers d'un pool public
     */
    @GetMapping("/files/pool/{poolId}/public")
    public ResponseEntity<?> getPublicPoolFiles(@PathVariable int poolId) {
        Pool pool = poolService.getPoolById(poolId);

        if (pool == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Pool non trouvé"));
        }

        if (pool.getPublicAccess() == null || !pool.getPublicAccess()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Ce pool n'est pas public"));
        }

        List<File> files = fileService.getFilesByPoolId(poolId);

        List<Map<String, Object>> filesData = files.stream()
                .map(file -> Map.of(
                        "id", file.getId(),
                        "fileName", file.getFileName(),
                        "filePath", file.getFilePath(),
                        "uploadedAt", file.getUploadedAt(),
                        "poolId", file.getPoolId()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(filesData);
    }
}
