package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.ENUM.UserStatus;
import com.Spring_chat.Web_chat.dto.user.UserSearchProjection;
import com.Spring_chat.Web_chat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByIdAndStatus(Long id, UserStatus status);

    /**
     * Tìm kiếm user theo username hoặc fullName (case-insensitive, substring).
     *
     * Logic loại trừ:
     *  - Chính mình (id = :me)
     *  - User BANNED / INACTIVE / BLOCKED
     *  - User đã block mình (để tránh lộ sự tồn tại của họ)
     *
     * Trả thêm friendRelation (NONE / FRIENDS / PENDING_SENT / PENDING_RECEIVED)
     * để FE render nút "Kết bạn" / "Đang chờ" / "Đã là bạn" ngay trong kết quả.
     *
     * Performance: cần trigram index trên username và "fullName".
     * Xem migration script trong /docs/db/V2__search_trigram_index.sql
     */
    @Query(
        value = """
            SELECT
                u.id                                         AS "id",
                u.username                                   AS "username",
                u."fullName"                                 AS "fullName",
                u."avatarUrl"                                AS "avatarUrl",
                CASE
                    WHEN EXISTS (
                        SELECT 1 FROM "Friendship" f
                        WHERE (
                            (f."requesterId" = :me AND f."addresseeId" = u.id)
                         OR (f."requesterId" = u.id AND f."addresseeId" = :me)
                        )
                        AND f.status = 'ACCEPTED'
                    ) THEN 'FRIENDS'
                    WHEN EXISTS (
                        SELECT 1 FROM "Friendship" f
                        WHERE f."requesterId" = :me
                          AND f."addresseeId" = u.id
                          AND f.status = 'PENDING'
                    ) THEN 'PENDING_SENT'
                    WHEN EXISTS (
                        SELECT 1 FROM "Friendship" f
                        WHERE f."requesterId" = u.id
                          AND f."addresseeId" = :me
                          AND f.status = 'PENDING'
                    ) THEN 'PENDING_RECEIVED'
                    ELSE 'NONE'
                END AS "friendRelation"
            FROM "User" u
            WHERE u.id         != :me
              AND u.status      = 'ACTIVE'
              AND (u.username ILIKE :q OR u."fullName" ILIKE :q)
              AND NOT EXISTS (
                  SELECT 1 FROM "Friendship" f
                  WHERE f.status        = 'BLOCKED'
                    AND f."requesterId" = u.id
                    AND f."addresseeId" = :me
              )
            ORDER BY u.username ASC
            """,
        countQuery = """
            SELECT COUNT(u.id)
            FROM "User" u
            WHERE u.id         != :me
              AND u.status      = 'ACTIVE'
              AND (u.username ILIKE :q OR u."fullName" ILIKE :q)
              AND NOT EXISTS (
                  SELECT 1 FROM "Friendship" f
                  WHERE f.status        = 'BLOCKED'
                    AND f."requesterId" = u.id
                    AND f."addresseeId" = :me
              )
            """,
        nativeQuery = true
    )
    Page<UserSearchProjection> searchUsers(
            @Param("q")  String   q,
            @Param("me") Long     me,
            Pageable              pageable
    );
}
