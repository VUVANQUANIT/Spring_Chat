package com.Spring_chat.Spring_chat.entity;

import com.Spring_chat.Spring_chat.ENUM.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "\"User\"")
@Check(constraints = "\"status\" IN ('ACTIVE', 'INACTIVE', 'BANNED')")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "\"username\"", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "\"email\"", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "\"passwordHash\"", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "\"fullName\"", length = 100)
    private String fullName;

    @Column(name = "\"avatarUrl\"")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "\"status\"", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "\"lastSeen\"")
    private Instant lastSeen;

    @CreationTimestamp
    @Column(name = "\"createdAt\"", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "\"updatedAt\"", nullable = false)
    private Instant updatedAt;
}
