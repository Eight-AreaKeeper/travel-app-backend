package com.travelapp.security;

import com.travelapp.common.BusinessException;
import com.travelapp.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Unified entry point for retrieving the current logged-in user's ID from the SecurityContext,
 * so each Controller doesn't have to parse Authentication itself.
 */
@Component
public class CurrentUserProvider {

    /** Used by endpoints that require login; throws 401 if the current user can't be resolved */
    public Long requireUserId() {
        Long userId = getUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /** Used by endpoints where login is optional (e.g. customize Q&A can be started without logging in); returns null if unresolved */
    public Long getUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return null;
        }
        return (Long) authentication.getPrincipal();
    }
}
