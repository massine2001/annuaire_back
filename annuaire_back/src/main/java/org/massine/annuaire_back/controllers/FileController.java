package org.massine.annuaire_back.controllers;

import org.massine.annuaire_back.config.SftpConfig;
import org.massine.annuaire_back.exceptions.ErrorResponse;
import org.massine.annuaire_back.models.File;
import org.massine.annuaire_back.models.Pool;
import org.massine.annuaire_back.models.User;
import org.massine.annuaire_back.services.AccessService;
import org.massine.annuaire_back.services.FileService;
import org.massine.annuaire_back.services.PoolService;
import org.massine.annuaire_back.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {
    

    private final FileService fileService;
    private final PoolService poolService;
    private final UserService userService;
    private final SftpConfig sftpConfig;
    private final AccessService accessService; 

    public FileController(
            FileService fileService,
            PoolService poolService,
            UserService userService,
            SftpConfig sftpConfig,
            AccessService accessService 
    ) {
        this.fileService = fileService;
        this.poolService = poolService;
        this.userService = userService;
        this.sftpConfig = sftpConfig;
        this.accessService = accessService;
    }


    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof String)) {
            return null;
        }
        String email = (String) authentication.getPrincipal();
        return userService.findByEmailSafe(email);
    }


    @GetMapping
    public ResponseEntity<List<File>> getAll() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Integer> accessiblePoolIds = accessService.getAccessiblePoolIds(currentUser.getId());

        List<File> allFiles = fileService.getAllFiles();
        List<File> accessibleFiles = allFiles.stream()
                .filter(file -> file.getPool() != null && accessiblePoolIds.contains(file.getPool().getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(accessibleFiles);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getFilesCount() {
        return ResponseEntity.ok(fileService.getFilesCount());
    }

    @GetMapping("/{id}")
    public ResponseEntity<File> getById(@PathVariable int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        File file = fileService.getFileById(id);

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        if (!accessService.userCanAccessFile(currentUser.getId(), file)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(file);
    }

    @GetMapping("/uploader/{id}")
    public ResponseEntity<User> getUploader(@PathVariable int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        File file = fileService.getFileById(id);

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        if (!accessService.userCanAccessFile(currentUser.getId(), file)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User uploader = fileService.findUploader(id);
        if (uploader == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(uploader);
    }


    @GetMapping("/pool/{id}")
    public ResponseEntity<Pool> getPool(@PathVariable int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        File file = fileService.getFileById(id);

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        if (!accessService.userCanAccessFile(currentUser.getId(), file)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Pool pool = fileService.findPoolById(id);
        if (pool == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(pool);
    }

    @PutMapping("/{id}")
    public ResponseEntity<File> updateFile(
            @PathVariable int id,
            @RequestParam(value = "file", required = false) MultipartFile newContent,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "expirationDate", required = false) String expirationDateStr) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        File existingFile = fileService.getFileById(id);

        if (existingFile == null) {
            return ResponseEntity.notFound().build();
        }

        if (!accessService.userCanAccessFile(currentUser.getId(), existingFile)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!accessService.userCanModifyInPool(currentUser.getId(), existingFile.getPool().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

        try {
            if (newContent != null && !newContent.isEmpty()) {
                String oldPath = existingFile.getPath();
                int lastSlash = oldPath.lastIndexOf('/');
                String remoteDir = (lastSlash > 0) ? oldPath.substring(0, lastSlash) : sftpConfig.normalizedBaseDir();

                String safeName = fileService.sanitizeFilename(newContent.getOriginalFilename());
                fileService.deleteRemote(oldPath);
                fileService.uploadToDir(remoteDir, safeName, newContent.getInputStream());
                existingFile.setName(safeName);
                existingFile.setPath(remoteDir + "/" + safeName);
            } else if (name != null && !name.isBlank()) {
                existingFile.setName(name);
            }

            if (description != null) {
                existingFile.setDescription(description);
            }

            if (expirationDateStr != null && !expirationDateStr.isBlank()) {
                try {
                    existingFile.setExpirationDate(LocalDate.parse(expirationDateStr));
                } catch (DateTimeParseException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
            }

            File updated = fileService.updateFileEntity(id, existingFile);
            return ResponseEntity.ok(updated);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        File file = fileService.getFileById(id);

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        if (!accessService.userCanAccessFile(currentUser.getId(), file)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!accessService.userCanModifyInPool(currentUser.getId(), file.getPool().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            fileService.deleteRemote(file.getPath());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        fileService.deleteFileById(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/upload")
    public ResponseEntity<File> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("poolId") int poolId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "expirationDate", required = false) String expirationDateStr) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Pool pool = poolService.getPoolById(poolId);

            if (pool == null) {
                return ResponseEntity.badRequest().build();
            }

            if (!accessService.userHasAccessToPool(currentUser.getId(), poolId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (!accessService.userCanModifyInPool(currentUser.getId(), poolId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String remoteDir = fileService.buildRemoteDirFor(poolId, currentUser.getId());
            String safeName = fileService.sanitizeFilename(file.getOriginalFilename());
            fileService.uploadToDir(remoteDir, safeName, file.getInputStream());

            String displayName = (name != null && !name.trim().isEmpty()) ? name : safeName;

            if (name != null && !name.trim().isEmpty()) {
                String originalExtension = getFileExtension(file.getOriginalFilename());
                if (originalExtension != null && !displayName.toLowerCase().endsWith(originalExtension.toLowerCase())) {
                    displayName = displayName + originalExtension;
                }
            }

            File savedFile = new File();
            savedFile.setName(displayName);
            savedFile.setPath(remoteDir + "/" + safeName);
            savedFile.setPool(pool);
            savedFile.setUserUploader(currentUser);
            savedFile.setCreatedAt(Instant.now());
            savedFile.setDescription(description);

            if (expirationDateStr != null && !expirationDateStr.isBlank()) {
                try {
                    savedFile.setExpirationDate(LocalDate.parse(expirationDateStr));
                } catch (DateTimeParseException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
            }

            File persisted = fileService.saveFile(savedFile);
            return ResponseEntity.status(HttpStatus.CREATED).body(persisted);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable int fileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        File file = fileService.getFileById(fileId);

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        if (!accessService.userCanAccessFile(currentUser.getId(), file)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            FileService.RemoteStream rs = fileService.getRemoteStream(file.getPath());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.attachment().filename(file.getName()).build());
            if (rs.length() >= 0) headers.setContentLength(rs.length());

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(rs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewFile(@PathVariable int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        File file = fileService.getFileById(id);

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        if (!accessService.userCanAccessFile(currentUser.getId(), file)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            FileService.RemoteStream rs = fileService.getRemoteStream(file.getPath());
            String fileName = file.getName().toLowerCase();
            MediaType contentType;

            if (fileName.endsWith(".pdf")) contentType = MediaType.APPLICATION_PDF;
            else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) contentType = MediaType.IMAGE_JPEG;
            else if (fileName.endsWith(".png")) contentType = MediaType.IMAGE_PNG;
            else if (fileName.endsWith(".gif")) contentType = MediaType.IMAGE_GIF;
            else if (fileName.endsWith(".svg")) contentType = MediaType.valueOf("image/svg+xml");
            else if (fileName.endsWith(".mp4")) contentType = MediaType.valueOf("video/mp4");
            else if (fileName.endsWith(".webm")) contentType = MediaType.valueOf("video/webm");
            else if (fileName.endsWith(".mp3")) contentType = MediaType.valueOf("audio/mpeg");
            else if (fileName.endsWith(".wav")) contentType = MediaType.valueOf("audio/wav");
            else if (fileName.endsWith(".txt")) contentType = MediaType.TEXT_PLAIN;
            else if (fileName.endsWith(".json")) contentType = MediaType.APPLICATION_JSON;
            else contentType = MediaType.APPLICATION_OCTET_STREAM;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.inline().filename(file.getName()).build());
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

    private String getFileExtension(String filename) {
        if (filename == null) return null;
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : null;
    }
}


