package com.example.demo.controllers;

import com.example.demo.config.SftpConfig;
import com.example.demo.models.File;
import com.example.demo.models.Pool;
import com.example.demo.models.User;
import com.example.demo.services.FileService;
import com.example.demo.services.PoolService;
import com.example.demo.services.UserService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileService fileService;
    private final PoolService poolService;
    private final UserService userService;
    private final SftpConfig sftpConfig;

    public FileController(FileService fileService, PoolService poolService, UserService userService, SftpConfig sftpConfig) {
        this.fileService = fileService;
        this.poolService = poolService;
        this.userService = userService;
        this.sftpConfig = sftpConfig;
    }

    @GetMapping
    public ResponseEntity<List<File>> getAll() {
        List<File> files = fileService.getAllFiles();
        return ResponseEntity.ok(files);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getFilesCount() {
        return ResponseEntity.ok(fileService.getFilesCount());
    }

    @GetMapping("/{id}")
    public ResponseEntity<File> getById(@PathVariable int id) {
        File file = fileService.getFileById(id);
        if (file == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(file);
    }

    @GetMapping("/uploader/{id}")
    public ResponseEntity<User> getUploader(@PathVariable int id) {
        User user = fileService.findUploader(id);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(user);
    }

    @GetMapping("/pool/{id}")
    public ResponseEntity<Pool> getPool(@PathVariable int id) {
        Pool pool = fileService.findPoolById(id);
        if (pool == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(pool);
    }

    @PutMapping("/{id}")
    public ResponseEntity<File> updateFile(
            @PathVariable int id,
            @RequestParam(value = "file", required = false) MultipartFile newContent,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "expirationDate", required = false) String expirationDateStr) {

        File existingFile = fileService.getFileById(id);
        if (existingFile == null) return ResponseEntity.notFound().build();

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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable int id) {
        File f = fileService.getFileById(id);
        if (f == null) return ResponseEntity.notFound().build();

        try {
            fileService.deleteRemote(f.getPath());
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
            @RequestParam("userId") int userId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "expirationDate", required = false) String expirationDateStr) {

        try {
            Pool pool = poolService.getPoolById(poolId);
            User user = userService.findById(userId);
            if (pool == null || user == null) return ResponseEntity.badRequest().build();

            String remoteDir = fileService.buildRemoteDirFor(poolId, userId);
            String safeName = fileService.sanitizeFilename(file.getOriginalFilename());
            fileService.uploadToDir(remoteDir, safeName, file.getInputStream());

            File savedFile = new File();
            savedFile.setName(safeName);
            savedFile.setPath(remoteDir + "/" + safeName);
            savedFile.setPool(pool);
            savedFile.setUserUploader(user);
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable int fileId) {
        File file = fileService.getFileById(fileId);
        if (file == null) return ResponseEntity.notFound().build();

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
        File file = fileService.getFileById(id);
        if (file == null) return ResponseEntity.notFound().build();

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
}
