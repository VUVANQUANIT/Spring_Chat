package com.Spring_chat.Web_chat.service.common;

import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import com.Spring_chat.Web_chat.repository.UserRepository;
import com.Spring_chat.Web_chat.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserProvider {
    private final UserRepository userRepository;
    public User findCurrentUserOrThrow() {
        AuthenticatedUser authenticatedUser =
                (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return findUserOrThrow(authenticatedUser.id());
    }

    public User findUserOrThrow(Long id) {
        if (id == null) {
            throw new AppException(ErrorCode.MISSING_PARAMETER, "Missing user id");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }
}
