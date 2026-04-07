package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.enums.RoleName;
import com.Spring_chat.Web_chat.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
    boolean existsByName(RoleName name);
}
