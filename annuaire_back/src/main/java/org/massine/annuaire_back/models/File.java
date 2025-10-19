package org.massine.annuaire_back.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "File",
        indexes = {
                @Index(name = "idx_file_pool", columnList = "pool_id"),
                @Index(name = "idx_file_uploader", columnList = "uploaded_by")
        })
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank
    @Column(name = "path", nullable = false, length = 1024)
    private String path;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_id", referencedColumnName = "id", nullable = false)
    private Pool pool;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", referencedColumnName = "id", nullable = false)
    private User userUploader;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Size(max = 10000)
    @Column(name = "description", length = 10000)
    private String description;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Pool getPool() { return pool; }
    public void setPool(Pool pool) { this.pool = pool; }

    public User getUserUploader() { return userUploader; }
    public void setUserUploader(User userUploader) { this.userUploader = userUploader; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
}
