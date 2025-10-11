package com.example.demo.controllers;

import com.example.demo.config.SftpConfig;
import com.example.demo.models.File;
import com.example.demo.models.Pool;
import com.example.demo.models.User;
import com.example.demo.services.FileService;
import com.example.demo.services.PoolService;
import com.example.demo.services.UserService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;

@RestController
@RequestMapping("/api/file/")
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

    @GetMapping("/")
    public ResponseEntity<List<File>> getAll(){
        List<File> files = fileService.getAllFiles();
        if(files.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(files);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getFilesCount() {
        long count = fileService.getFilesCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}")
    public ResponseEntity<File> getById(@PathVariable int id){
        File file = fileService.getFileById(id);
        if(file == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(file);
    }

    @GetMapping("/path/{id}")
    public ResponseEntity<String> getPath(@PathVariable int id){
        String path = fileService.findPath(id);
        if(path == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(path);
    }

    @GetMapping("/uploader/{id}")
    public ResponseEntity<User> getUploader(@PathVariable int id){
        User user = fileService.findUploader(id);
        if(user == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping("/pool/{id}")
    public ResponseEntity<Pool> getPool(@PathVariable int id){
        Pool pool = fileService.findPoolById(id);
        if(pool == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pool);
    }

    @PostMapping("/")
    public ResponseEntity<File> createFile(@RequestBody File file){
        File createdFile = fileService.saveFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFile);
    }

    @PutMapping("/{id}")
    public ResponseEntity<File> updateFile(@PathVariable int id, @RequestBody File file){
        File fileUpdated = fileService.updateFile(id, file);
        return ResponseEntity.ok(fileUpdated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<File> deleteFile(@PathVariable int id){
        if(fileService.getFileById(id) == null){
            return ResponseEntity.notFound().build();
        }
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("poolId") int poolId,
            @RequestParam("userId") int userId) {

        try {
            Pool pool = poolService.getPoolById(poolId);
            User user = userService.findById(userId);
            if (pool == null || user == null) {
                return ResponseEntity.badRequest().body("Invalid Pool or User ID");
            }

            JSch jsch = new JSch();
            jsch.addIdentity(sftpConfig.getPrivateKeyPath());
            Session session = jsch.getSession(
                    sftpConfig.getUsername(),
                    sftpConfig.getHost(),
                    sftpConfig.getPort()
            );
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            String remoteDir = sftpConfig.getBaseDirectory() + "/pool" + poolId + "/user" + userId;

            String[] folders = remoteDir.split("/");
            String path = "";
            for (String folder : folders) {
                if (folder.isEmpty()) continue;
                path += "/" + folder;
                try {
                    channelSftp.cd(path);
                } catch (Exception e) {
                    channelSftp.mkdir(path);
                    channelSftp.cd(path);
                }
            }

            InputStream inputStream = file.getInputStream();
            channelSftp.put(inputStream, file.getOriginalFilename());

            inputStream.close();
            channelSftp.disconnect();
            session.disconnect();

            File savedFile = new File();
            savedFile.setName(file.getOriginalFilename());
            savedFile.setPath(remoteDir + "/" + file.getOriginalFilename());
            savedFile.setPool(pool);
            savedFile.setUserUploader(user);
            savedFile.setCreatedAt(Instant.now());
            fileService.saveFile(savedFile);

            return ResponseEntity.ok("File uploaded successfully to: " + remoteDir);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed");
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable int fileId) {
        File file = fileService.findById(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            JSch jsch = new JSch();
            jsch.addIdentity(sftpConfig.getPrivateKeyPath());
            Session session = jsch.getSession(
                    sftpConfig.getUsername(),
                    sftpConfig.getHost(),
                    sftpConfig.getPort()
            );
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            InputStream inputStream = channelSftp.get(file.getPath());
            byte[] fileBytes = inputStream.readAllBytes();

            inputStream.close();
            channelSftp.disconnect();
            session.disconnect();

            Resource resource = new org.springframework.core.io.ByteArrayResource(fileBytes);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewFile(@PathVariable Long id) {
        try {
            File file = fileService.getFileById(id.intValue());

            if (file == null) {
                return ResponseEntity.notFound().build();
            }

            JSch jsch = new JSch();
            jsch.addIdentity(sftpConfig.getPrivateKeyPath());
            Session session = jsch.getSession(
                    sftpConfig.getUsername(),
                    sftpConfig.getHost(),
                    sftpConfig.getPort()
            );
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            InputStream inputStream = channelSftp.get(file.getPath());
            byte[] fileBytes = inputStream.readAllBytes();

            inputStream.close();
            channelSftp.disconnect();
            session.disconnect();

            Resource resource = new org.springframework.core.io.ByteArrayResource(fileBytes);

            String fileName = file.getName().toLowerCase();
            String contentType;

            if (fileName.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (fileName.endsWith(".png")) {
                contentType = "image/png";
            } else if (fileName.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (fileName.endsWith(".svg")) {
                contentType = "image/svg+xml";
            } else if (fileName.endsWith(".mp4")) {
                contentType = "video/mp4";
            } else if (fileName.endsWith(".webm")) {
                contentType = "video/webm";
            } else if (fileName.endsWith(".mp3")) {
                contentType = "audio/mpeg";
            } else if (fileName.endsWith(".wav")) {
                contentType = "audio/wav";
            } else if (fileName.endsWith(".txt")) {
                contentType = "text/plain";
            } else if (fileName.endsWith(".json")) {
                contentType = "application/json";
            } else {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Type, Content-Disposition")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}