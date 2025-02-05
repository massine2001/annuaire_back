package com.example.demo.services;

import com.example.demo.models.File;
import com.example.demo.models.Pool;
import com.example.demo.models.User;
import com.example.demo.repositories.FileRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileService {
    private final FileRepository fileRepository;
    public FileService(FileRepository fileRepository){
        this.fileRepository = fileRepository;
    }

    public List<File> getAllFiles() {
        return fileRepository.findAll();
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

}
