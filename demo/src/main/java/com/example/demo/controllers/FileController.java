package com.example.demo.controllers;

import com.example.demo.models.File;
import com.example.demo.models.Pool;
import com.example.demo.models.User;
import com.example.demo.services.FileService;
import com.example.demo.services.PoolService;
import com.example.demo.services.UserService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

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
    public FileController(FileService fileService, PoolService poolService, UserService userService) {
        this.fileService = fileService;
        this.poolService = poolService;
        this.userService = userService;
    }

    @GetMapping("/")
    public ResponseEntity<List<File>> getAll(){
        List<File> files = fileService.getAllFiles();
        if(files.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(files);
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
            // Vérifiez si le pool et l'utilisateur existent
            Pool pool = poolService.getPoolById(poolId);
            User user = userService.findById(userId);
            if (pool == null || user == null) {
                return ResponseEntity.badRequest().body("Invalid Pool or User ID");
            }

            // Initialiser JSch et établir une connexion SFTP
            JSch jsch = new JSch();
            jsch.addIdentity("C:/Users/aghar/.ssh/id_rsa");
            Session session = jsch.getSession("logiuqkd", "world-365.fr.planethoster.net", 5022);

            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();


            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            String remoteDir = "/home/logiuqkd/annuaire/pool" + poolId + "/user" + userId;

            String[] folders = remoteDir.split("/");
            String path = "";
            try {
                for (String folder : folders) {
                    if (folder.isEmpty()) continue; // Sauter les parties vides
                    path += "/" + folder;
                    try {
                        channelSftp.cd(path); // Essayer d'accéder au dossier
                    } catch (Exception e) {
                        channelSftp.mkdir(path); // Si non existant, le créer
                        channelSftp.cd(path);   // Se déplacer dans le nouveau dossier
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


            // Sauvegarder le fichier
            InputStream inputStream = file.getInputStream();
            channelSftp.put(inputStream, file.getOriginalFilename());

            // Fermer les connexions
            inputStream.close();
            channelSftp.disconnect();
            session.disconnect();

            // Enregistrer les informations dans la BDD
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

}
