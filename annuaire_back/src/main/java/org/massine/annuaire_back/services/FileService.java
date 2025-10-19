package org.massine.annuaire_back.services;

import org.massine.annuaire_back.config.SftpConfig;
import org.massine.annuaire_back.models.File;
import org.massine.annuaire_back.models.Pool;
import org.massine.annuaire_back.models.User;
import org.massine.annuaire_back.repositories.FileRepository;
import com.jcraft.jsch.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;


@Service
public class FileService {

    private final SftpConfig sftpConfig;
    private final FileRepository fileRepository;

    // Pool de sessions SFTP pour réutilisation
    private static final ConcurrentLinkedQueue<Session> sessionPool = new ConcurrentLinkedQueue<>();
    private static final int MAX_POOL_SIZE = 5; // Limite pour éviter une consommation mémoire excessive

    public FileService(SftpConfig sftpConfig, FileRepository fileRepository) {
        this.sftpConfig = sftpConfig;
        this.fileRepository = fileRepository;
    }


    public List<File> getAllFiles() { return fileRepository.findAll(); }

    public long getFilesCount() { return fileRepository.count(); }

    public File getFileById(int id) { return fileRepository.findById(id).orElse(null); }

    public Optional<File> getOptional(int id) { return fileRepository.findById(id); }

    public Pool findPoolById(int fileId) { return fileRepository.findPoolById(fileId); }
    public List<File> findByPoolId(int poolId) {
        return fileRepository.findByPoolId(poolId);
    }
    public String findPath(int id) { return fileRepository.findPath(id); }

    public User findUploader(int fileId) { return fileRepository.findUploader(fileId); }

    public File saveFile(File file) { return fileRepository.save(file); }

    public void deleteFileById(int id) { fileRepository.deleteById(id); }

    @Transactional
    public File updateFileEntity(int id, File patch) {
        File mf = fileRepository.findById(id).orElse(null);
        if (mf == null) return null;
        if (patch.getName() != null) mf.setName(patch.getName());
        if (patch.getDescription() != null) mf.setDescription(patch.getDescription());
        if (patch.getExpirationDate() != null) mf.setExpirationDate(patch.getExpirationDate());
        if (patch.getPath() != null) mf.setPath(patch.getPath());
        if (patch.getPool() != null) mf.setPool(patch.getPool());
        if (patch.getUserUploader() != null) mf.setUserUploader(patch.getUserUploader());
        return fileRepository.save(mf);
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
        session.setConfig("compression.s2c", "none");
        session.setConfig("compression.c2s", "none");
        session.connect();
        return session;
    }

    private ChannelSftp openSftp(Session session) throws JSchException {
        ChannelSftp ch = (ChannelSftp) session.openChannel("sftp");
        ch.connect();
        return ch;
    }

    public void ensureDirectory(ChannelSftp sftp, String absoluteDir) throws SftpException {
        String[] parts = absoluteDir.split("/");
        String path = "";
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            path += "/" + p;
            try {
                sftp.cd(path);
            } catch (SftpException e) {
                sftp.mkdir(path);
                sftp.cd(path);
            }
        }
    }

    public String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) return "unnamed";
        
        String cleaned = original.replace("\\", "/");
        int slash = cleaned.lastIndexOf('/');
        if (slash >= 0) cleaned = cleaned.substring(slash + 1);
        
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9._\\-\\s]", "_");
        
        cleaned = cleaned.replaceAll("\\.{2,}", ".");  // Remplacer .. par .
        cleaned = cleaned.trim();
        
        if (cleaned.isEmpty() || cleaned.matches("^\\.+$")) {
            return "unnamed";
        }
        
        if (cleaned.length() > 255) {
            String extension = "";
            int lastDot = cleaned.lastIndexOf('.');
            if (lastDot > 0) {
                extension = cleaned.substring(lastDot);
                cleaned = cleaned.substring(0, Math.min(255 - extension.length(), lastDot));
            } else {
                cleaned = cleaned.substring(0, 255);
            }
            cleaned = cleaned + extension;
        }
        
        return cleaned;
    }

    public void uploadToDir(String remoteDir, String filename, InputStream data) throws Exception {
        long startTime = System.currentTimeMillis();
        Session session = null;
        ChannelSftp sftp = null;
        try (InputStream in = data) {
            long sessionStart = System.currentTimeMillis();
            session = getSession();

            long sftpStart = System.currentTimeMillis();
            sftp = openSftp(session);

            long dirStart = System.currentTimeMillis();
            ensureDirectory(sftp, remoteDir);

            sftp.cd(remoteDir);

            long putStart = System.currentTimeMillis();
            sftp.put(in, filename);
        } finally {
            if (sftp != null) sftp.disconnect();
            returnSession(session);
        }
    }

    public void deleteRemote(String remotePath) throws Exception {
        Session session = null;
        ChannelSftp sftp = null;
        try {
            session = createSession();
            sftp = openSftp(session);
            try {
                sftp.rm(remotePath);
            } catch (SftpException ignore) {}
        } finally {
            if (sftp != null) sftp.disconnect();
            if (session != null) session.disconnect();
        }
    }

    public RemoteStream getRemoteStream(String remotePath) throws Exception {
        Session session = createSession();
        ChannelSftp sftp = openSftp(session);
        try {
            SftpATTRS attrs = sftp.lstat(remotePath);
            InputStream in = sftp.get(remotePath);
            return new RemoteStream(in, attrs.getSize(), sftp, session);
        } catch (Exception e) {
            sftp.disconnect();
            session.disconnect();
            throw e;
        }
    }

    public static class RemoteStream extends InputStreamResource {
        private final long length;
        private final ChannelSftp sftp;
        private final Session session;
        public RemoteStream(InputStream inputStream, long length, ChannelSftp sftp, Session session) {
            super(inputStream);
            this.length = length;
            this.sftp = sftp;
            this.session = session;
        }
        public long length() { return length; }
        public void close() {
            try { super.getInputStream().close(); } catch (Exception ignore) {}
            try { sftp.disconnect(); } catch (Exception ignore) {}
            try { session.disconnect(); } catch (Exception ignore) {}
        }
    }

    public String buildRemoteDirFor(int poolId, int userId) {
        return sftpConfig.normalizedBaseDir() + "/pool" + poolId + "/user" + userId;
    }

    public void uploadFileToPoolUser(int poolId, int userId, MultipartFile file) throws Exception {
        String remoteDir = buildRemoteDirFor(poolId, userId);
        String filename = sanitizeFilename(file.getOriginalFilename());
        uploadToDir(remoteDir, filename, file.getInputStream());
    }

    private Session getSession() throws JSchException {
        Session session = sessionPool.poll();
        if (session != null && session.isConnected()) {
            return session;
        }
        // Si la session du pool n'est pas valide, créer une nouvelle
        if (session != null) {
            session.disconnect(); // Fermer la session invalide
        }
        JSch jsch = new JSch();
        jsch.addIdentity(sftpConfig.getPrivateKeyPath());
        session = jsch.getSession(
                sftpConfig.getUsername(),
                sftpConfig.getHost(),
                sftpConfig.getPort()
        );
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("compression.s2c", "none");
        session.setConfig("compression.c2s", "none");
        session.connect();
        return session;
    }

    private void returnSession(Session session) {
        if (session != null && session.isConnected() && sessionPool.size() < MAX_POOL_SIZE) {
            sessionPool.offer(session);
        } else {
            if (session != null) session.disconnect();
        }
    }
}
