package com.Spring_chat.Web_chat.service.user;

import com.Spring_chat.Web_chat.enums.FriendRelation;
import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.PageResponse;
import com.Spring_chat.Web_chat.dto.user.MyProfileUserDTO;
import com.Spring_chat.Web_chat.dto.user.ProfileUserDTO;
import com.Spring_chat.Web_chat.dto.user.UpdateMyProfileRequestDTO;
import com.Spring_chat.Web_chat.dto.user.UserSearchProjection;
import com.Spring_chat.Web_chat.dto.user.UserSearchResponseDTO;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import com.Spring_chat.Web_chat.mappers.UserMapper;
import com.Spring_chat.Web_chat.repository.UserRepository;
import com.Spring_chat.Web_chat.service.common.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserRepository       userRepository;
    private final UserMapper           userMapper;
    private final CurrentUserProvider  currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<ProfileUserDTO> getUserProfile(Long id) {
        User user = currentUserProvider.findUserOrThrow(id);
        return ApiResponse.ok("OK", userMapper.userToUserDTO(user));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<MyProfileUserDTO> getMyProfile() {
        User user = currentUserProvider.findCurrentUserOrThrow();
        return ApiResponse.ok("OK", userMapper.userToMyUserDTO(user));
    }

    @Override
    @Transactional
    public ApiResponse<MyProfileUserDTO> updateMyProfile(UpdateMyProfileRequestDTO request) {
        User user = currentUserProvider.findCurrentUserOrThrow();

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        User savedUser = userRepository.save(user);
        return ApiResponse.ok("Profile updated successfully", userMapper.userToMyUserDTO(savedUser));
    }

    /**
     * Tìm kiếm user theo username hoặc fullName (case-insensitive, substring match).
     *
     * Tham số q được bao bởi ký tự % để cho phép ILIKE '%q%'.
     * Performance: query dựa vào trigram index (pg_trgm). Nếu chưa có index,
     * cần chạy migration V2__search_trigram_index.sql trước.
     *
     * Giới hạn q: tối thiểu 2 ký tự (kiểm tra ở đây, không ở controller)
     * để tránh query quá rộng trên toàn bảng.
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<UserSearchResponseDTO>> searchUsers(String q, int page, int size) {
        if (q == null || q.isBlank()) {
            throw new AppException(ErrorCode.MISSING_PARAMETER, "Từ khóa tìm kiếm không được để trống");
        }
        String trimmed = q.trim();
        if (trimmed.length() < 2) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Từ khóa phải có ít nhất 2 ký tự");
        }

        // Clamp size về giới hạn an toàn
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        Long currentUserId = currentUserProvider.findCurrentUserOrThrow().getId();

        // ILIKE '%keyword%' — được tăng tốc bởi pg_trgm GIN index
        String likePattern = "%" + trimmed + "%";

        Page<UserSearchProjection> resultPage = userRepository.searchUsers(
                likePattern,
                currentUserId,
                PageRequest.of(safePage, safeSize)
        );

        List<UserSearchResponseDTO> content = resultPage.getContent()
                .stream()
                .map(this::toSearchResponseDTO)
                .toList();

        return ApiResponse.ok("OK", PageResponse.of(content, resultPage));
    }

    private UserSearchResponseDTO toSearchResponseDTO(UserSearchProjection projection) {
        return UserSearchResponseDTO.builder()
                .id(projection.getId())
                .username(projection.getUsername())
                .fullName(projection.getFullName())
                .avatarUrl(projection.getAvatarUrl())
                .friendRelation(parseFriendRelation(projection.getFriendRelation()))
                .build();
    }

    /**
     * Parse string từ CASE WHEN trong native query sang enum.
     * Dùng valueOf với fallback để đề phòng lỗi bất ngờ.
     */
    private FriendRelation parseFriendRelation(String value) {
        if (value == null) return FriendRelation.NONE;
        try {
            return FriendRelation.valueOf(value);
        } catch (IllegalArgumentException e) {
            return FriendRelation.NONE;
        }
    }
}
