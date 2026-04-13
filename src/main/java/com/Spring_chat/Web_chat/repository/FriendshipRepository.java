package com.Spring_chat.Web_chat.repository;

import com.Spring_chat.Web_chat.dto.friendship.FriendResponseDTO;
import com.Spring_chat.Web_chat.entity.Friendship;
import com.Spring_chat.Web_chat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.Spring_chat.Web_chat.enums.FriendshipStatus;

import java.util.Optional;


@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    boolean existsByRequester_IdAndAddressee_IdAndStatus(Long requesterId, Long addresseeId, FriendshipStatus status);

    // ─── Unified pageable query (spec 3.2) ───────────────────────────────────

    /**
     * Lấy danh sách friend requests với filter linh hoạt.
     *
     * @param userId   ID của user hiện tại
     * @param received true  → direction=RECEIVED (mình là addressee)
     *                 false → direction=SENT     (mình là requester)
     * @param status   null  → không lọc theo status (tất cả)
     *                 not-null → chỉ lấy bản ghi có status đó
     * @param pageable Spring Pageable (page, size, sort)
     */
    @Query("""
    select new com.Spring_chat.Web_chat.dto.friendship.FriendResponseDTO(
        f.id,
        f.requester.id,
        f.requester.username,
        f.requester.avatarUrl,
        f.addressee.id,
        f.addressee.username,
        f.addressee.avatarUrl,
        f.status,
        f.createdAt,
        f.updatedAt
    )
    from Friendship f
    where ((:received = true  and f.addressee.id  = :userId)
        or (:received = false and f.requester.id  = :userId))
    and (:status is null or f.status = :status)
    order by f.createdAt DESC
    """)
    Page<FriendResponseDTO> findRequests(
            @Param("userId")   Long userId,
            @Param("received") boolean received,
            @Param("status")   FriendshipStatus status,
            Pageable pageable
    );

    Friendship findByRequesterAndAddressee(User requester, User addressee);

    @Query("""
    SELECT f FROM Friendship f
    WHERE (f.requester.id = :userAId AND f.addressee.id = :userBId)
       OR (f.requester.id = :userBId AND f.addressee.id = :userAId)
    """)
    Optional<Friendship> findBetweenUsers(
            @Param("userAId") Long userAId,
            @Param("userBId") Long userBId
    );
}
