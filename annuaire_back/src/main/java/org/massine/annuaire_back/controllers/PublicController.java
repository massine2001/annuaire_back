package org.massine.annuaire_back.controllers;

import org.massine.annuaire_back.models.File;
import org.massine.annuaire_back.models.Pool;
import org.massine.annuaire_back.services.FileService;
import org.massine.annuaire_back.services.PoolService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
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
     * RÃ©cupÃ©rer la liste des pools publics (pour les visiteurs non connectÃ©s)
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
                            "fileCount", fileService.findByPoolId(pool.getId()).size(),
                            "isPublic", true
                    );
                    return poolData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(poolsWithFileCount);
    }

    /**
     * RÃ©cupÃ©rer les dÃ©tails d'un pool public spÃ©cifique
     */
    @GetMapping("/pools/{poolId}/public")
    public ResponseEntity<?> getPublicPoolDetails(@PathVariable int poolId) {
        Pool pool = poolService.getPoolById(poolId);

        if (pool == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Pool non trouvÃ©"));
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
     * RÃ©cupÃ©rer les fichiers d'un pool public
     */
    @GetMapping("/files/pool/{poolId}/public")
    public ResponseEntity<?> getPublicPoolFiles(@PathVariable int poolId) {
        Pool pool = poolService.getPoolById(poolId);

        if (pool == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Pool non trouvÃ©"));
        }

        if (pool.getPublicAccess() == null || !pool.getPublicAccess()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Ce pool n'est pas public"));
        }

        List<File> files = fileService.findByPoolId(poolId);

        List<Map<String, Object>> filesData = files.stream()
                .map(file -> {
                    Map<String, Object> fileData = new java.util.HashMap<>();
                    fileData.put("id", file.getId());
                    fileData.put("name", file.getName());
                    fileData.put("path", file.getPath());
                    fileData.put("createdAt", file.getCreatedAt());
                    fileData.put("pool", Map.of("id", file.getPool().getId()));
                    if (file.getUserUploader() != null) {
                        fileData.put("userUploader", Map.of(
                                "id", file.getUserUploader().getId(),
                                "firstName", file.getUserUploader().getFirstName(),
                                "lastName", file.getUserUploader().getLastName(),
                                "email", file.getUserUploader().getEmail()
                        ));
                    }
                    return fileData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(filesData);
    }

    /**
     * TÃ©lÃ©charger un fichier d'un pool public
     */
    @GetMapping("/files/download/{fileId}/public")
    public ResponseEntity<Resource> downloadPublicFile(@PathVariable int fileId) {
        File file = fileService.getFileById(fileId);

        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }

        Pool pool = file.getPool();
        if (pool.getPublicAccess() == null || !pool.getPublicAccess()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

        try {
            FileService.RemoteStream rs = fileService.getRemoteStream(file.getPath());
            HttpHeaders headers = new HttpHeaders();
            String filename = file.getName();
            if (filename == null || filename.isBlank()) {
                filename = "download";
            }
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
            if (rs.length() >= 0) headers.setContentLength(rs.length());

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(rs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PrÃ©visualiser un fichier d'un pool public
     */
    @GetMapping("/files/preview/{fileId}/public")
    public ResponseEntity<Resource> previewPublicFile(@PathVariable int fileId) {
        File file = fileService.getFileById(fileId);

        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }

        Pool pool = file.getPool();
        if (pool.getPublicAccess() == null || !pool.getPublicAccess()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

        try {
            FileService.RemoteStream rs = fileService.getRemoteStream(file.getPath());
            String fileName = file.getName();
            if (fileName == null || fileName.isBlank()) {
                fileName = "preview";
            }
            String lowerFileName = fileName.toLowerCase();
            MediaType contentType;

            if (lowerFileName.endsWith(".pdf")) contentType = MediaType.APPLICATION_PDF;
            else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) contentType = MediaType.IMAGE_JPEG;
            else if (lowerFileName.endsWith(".png")) contentType = MediaType.IMAGE_PNG;
            else if (lowerFileName.endsWith(".gif")) contentType = MediaType.IMAGE_GIF;
            else if (lowerFileName.endsWith(".svg")) contentType = MediaType.valueOf("image/svg+xml");
            else if (lowerFileName.endsWith(".mp4")) contentType = MediaType.valueOf("video/mp4");
            else if (lowerFileName.endsWith(".webm")) contentType = MediaType.valueOf("video/webm");
            else if (lowerFileName.endsWith(".mp3")) contentType = MediaType.valueOf("audio/mpeg");
            else if (lowerFileName.endsWith(".wav")) contentType = MediaType.valueOf("audio/wav");
            else if (lowerFileName.endsWith(".txt")) contentType = MediaType.TEXT_PLAIN;
            else if (lowerFileName.endsWith(".json")) contentType = MediaType.APPLICATION_JSON;
            else contentType = MediaType.APPLICATION_OCTET_STREAM;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.inline().filename(fileName).build());
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Type, Content-Disposition");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            if (rs.length() >= 0) headers.setContentLength(rs.length());

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(contentType)
                    .body(rs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
