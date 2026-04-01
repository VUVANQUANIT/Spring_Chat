package com.Spring_chat.Spring_chat.repository;

import com.Spring_chat.Spring_chat.dto.friendship.FriendResponseDTO;
import com.Spring_chat.Spring_chat.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.Spring_chat.Spring_chat.ENUM.FriendshipStatus;

import java.util.List;


@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    boolean existsByRequester_IdAndAddressee_IdAndStatus(Long requesterId, Long addresseeId, FriendshipStatus status);
    @Query("""
    select new com.Spring_chat.Spring_chat.dto.friendship.FriendResponseDTO(
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
    where f.addressee.id = :id
    and f.status = :status
    """)
    List<FriendResponseDTO> findAllRequestFriends(@Param("id") Long id, @Param("status") FriendshipStatus status);

    @Query("""
    select new com.Spring_chat.Spring_chat.dto.friendship.FriendResponseDTO(
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
    where f.requester.id = :id
    and f.status = :status
    """)
    List<FriendResponseDTO> findAllSentRequestFriends(@Param("id") Long id, @Param("status") FriendshipStatus status);
}
