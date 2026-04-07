package com.Spring_chat.Web_chat.config;

import com.Spring_chat.Web_chat.enums.PermissionName;
import com.Spring_chat.Web_chat.enums.RoleName;
import com.Spring_chat.Web_chat.entity.Permission;
import com.Spring_chat.Web_chat.entity.Role;
import com.Spring_chat.Web_chat.repository.PermissionRepository;
import com.Spring_chat.Web_chat.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.Spring_chat.Web_chat.enums.PermissionName.*;

/**
 * Runs once on startup to seed Roles and Permissions if they do not yet exist.
 * Idempotent — safe to re-run on every boot.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializePermissions();
        initializeRoles();
        log.info("DataInitializer: Roles and Permissions are ready.");
    }

    // ─── Permissions ──────────────────────────────────────────────────────────

    private void initializePermissions() {
        for (PermissionName pn : PermissionName.values()) {
            if (!permissionRepository.existsByName(pn)) {
                permissionRepository.save(
                        Permission.builder()
                                .name(pn)
                                .description(descriptionFor(pn))
                                .build()
                );
                log.debug("Created permission: {}", pn);
            }
        }
    }

    private String descriptionFor(PermissionName pn) {
        return switch (pn) {
            case USER_READ -> "View user profiles";
            case USER_WRITE -> "Edit own profile";
            case USER_DELETE -> "Delete any user account (admin)";
            case USER_BAN -> "Ban / unban users";
            case MESSAGE_READ -> "Read messages in conversations";
            case MESSAGE_WRITE -> "Send messages";
            case MESSAGE_DELETE -> "Delete own messages";
            case MESSAGE_DELETE_ANY -> "Delete any message (moderator/admin)";
            case CONVERSATION_READ -> "View conversations";
            case CONVERSATION_WRITE -> "Create and edit conversations";
            case CONVERSATION_DELETE -> "Delete conversations";
            case FRIENDSHIP_READ -> "View friend lists";
            case FRIENDSHIP_WRITE -> "Send / accept friend requests";
            case ADMIN_PANEL_ACCESS -> "Access the administration panel";
            case ROLE_MANAGE -> "Manage user roles (admin only)";
        };
    }

    // ─── Roles ────────────────────────────────────────────────────────────────

    private void initializeRoles() {
        // ROLE_USER — default role assigned on registration
        createRoleIfAbsent(RoleName.ROLE_USER, "Standard user", Set.of(
                USER_READ, USER_WRITE,
                MESSAGE_READ, MESSAGE_WRITE, MESSAGE_DELETE,
                CONVERSATION_READ, CONVERSATION_WRITE,
                FRIENDSHIP_READ, FRIENDSHIP_WRITE
        ));

        // ROLE_MODERATOR — can moderate content and ban users
        createRoleIfAbsent(RoleName.ROLE_MODERATOR, "Content moderator", Set.of(
                USER_READ, USER_WRITE, USER_BAN,
                MESSAGE_READ, MESSAGE_WRITE, MESSAGE_DELETE, MESSAGE_DELETE_ANY,
                CONVERSATION_READ, CONVERSATION_WRITE, CONVERSATION_DELETE,
                FRIENDSHIP_READ, FRIENDSHIP_WRITE,
                ADMIN_PANEL_ACCESS
        ));

        // ROLE_ADMIN — unrestricted access
        createRoleIfAbsent(RoleName.ROLE_ADMIN, "System administrator",
                new HashSet<>(Arrays.asList(PermissionName.values()))
        );
    }

    private void createRoleIfAbsent(RoleName roleName, String description, Set<PermissionName> permissionNames) {
        if (roleRepository.existsByName(roleName)) {
            return;
        }
        Set<Permission> permissions = permissionNames.stream()
                .map(pn -> permissionRepository.findByName(pn)
                        .orElseThrow(() -> new IllegalStateException("Permission not seeded: " + pn)))
                .collect(Collectors.toSet());

        roleRepository.save(
                Role.builder()
                        .name(roleName)
                        .description(description)
                        .permissions(permissions)
                        .build()
        );
        log.info("Created role: {}", roleName);
    }
}
