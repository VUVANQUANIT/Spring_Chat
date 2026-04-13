package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.enums.PermissionName;
import com.Spring_chat.Web_chat.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(PermissionName name);
    boolean existsByName(PermissionName name);
}
