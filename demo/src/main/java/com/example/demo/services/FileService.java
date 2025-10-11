package com.example.demo.services;

import com.example.demo.config.SftpConfig;
import com.example.demo.models.File;
import com.example.demo.models.Pool;
import com.example.demo.models.User;
import com.example.demo.repositories.FileRepository;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class FileService {
    private final SftpConfig sftpConfig;

    private final FileRepository fileRepository;
    public FileService(SftpConfig sftpConfig, FileRepository fileRepository){
        this.sftpConfig = sftpConfig;
        this.fileRepository = fileRepository;
    }
    private Session createSession() throws JSchException {
        JSch jsch = new JSch();
        jsch.addIdentity(sftpConfig.getPrivateKeyPath());

        Session session = jsch.getSession(
                sftpConfig.getUsername(),
                sftpConfig.getHost(),
                sftpConfig.getPort()
        );

        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        return session;
    }
    public List<File> getAllFiles() {
        return fileRepository.findAll();
    }
    public long getFilesCount() {
        return fileRepository.count();
    }
    public File getFileById(int id) {
        return fileRepository.findById(id);
    }
    public Pool findPoolById(int file_id){
        return fileRepository.findPoolById(file_id);
    }
    public String findPath(int id){
        return fileRepository.findPath(id);
    }
    public User findUploader(int file_id){
        return fileRepository.findUploader(file_id);
    }
    public File saveFile(File file){
        return fileRepository.save(file);
    }
    public void deleteFile(int id){
        fileRepository.deleteById(id);
    }
    public File updateFile(int id,File file){
        File modifiedFile = fileRepository.findById(id);
        if(file.getName() != null){
            modifiedFile.setName(file.getName());
        }
        if(file.getPath() != null){
            modifiedFile.setPath(file.getPath());
        }
        if(file.getPool() != null){
            modifiedFile.setPool(file.getPool());
        }
        if(file.getPool() != null){
            modifiedFile.setPool(file.getPool());
        }
        return fileRepository.save(modifiedFile);
    }
    public List<File> findByPoolId(int poolId) {
        return fileRepository.findByPoolId(poolId);
    }
    public File findById(int id) {
        return fileRepository.findById(id);
    }
    public void uploadFile(String poolName, String username, MultipartFile file) throws Exception {
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            session = createSession();
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // Créer le chemin distant
            String remotePath = sftpConfig.getBaseDirectory() + "/" + poolName + "/" + username + "/";

            // Créer les répertoires si nécessaire
            String[] folders = remotePath.split("/");
            String currentPath = "";
            for (String folder : folders) {
                if (!folder.isEmpty()) {
                    currentPath += "/" + folder;
                    try {
                        channelSftp.cd(currentPath);
                    } catch (Exception e) {
                        channelSftp.mkdir(currentPath);
                        channelSftp.cd(currentPath);
                    }
                }
            }

            channelSftp.put(file.getInputStream(), file.getOriginalFilename());

        } finally {
            if (channelSftp != null) channelSftp.disconnect();
            if (session != null) session.disconnect();
        }
    }




}
