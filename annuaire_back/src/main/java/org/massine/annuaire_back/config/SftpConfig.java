package com.example.demo.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "sftp")
@Validated
public class SftpConfig {
    @NotBlank
    private String host;

    @Min(1)
    private int port;

    @NotBlank
    private String username;

    @NotBlank
    private String privateKeyPath;

    @NotBlank
    private String baseDirectory;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }

    public String getBaseDirectory() { return baseDirectory; }
    public void setBaseDirectory(String baseDirectory) { this.baseDirectory = baseDirectory; }

    /** Base dir sans trailing slash */
    public String normalizedBaseDir() {
        return baseDirectory != null && baseDirectory.endsWith("/")
                ? baseDirectory.substring(0, baseDirectory.length() - 1)
                : baseDirectory;
    }
}
