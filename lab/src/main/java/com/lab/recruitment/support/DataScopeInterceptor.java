package com.lab.recruitment.support;

import com.lab.recruitment.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataScopeInterceptor implements HandlerInterceptor {

    @Autowired
    private CurrentUserAccessor currentUserAccessor;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return true;
        }

        try {
            User currentUser = currentUserAccessor.getCurrentUser();
            request.setAttribute(CurrentUserAccessor.CURRENT_USER_REQUEST_ATTRIBUTE, currentUser);
            request.setAttribute(CurrentUserAccessor.DATA_SCOPE_REQUEST_ATTRIBUTE,
                    currentUserAccessor.buildDataScope(currentUser));
        } catch (Exception ignored) {
            // Security rules still apply later. The interceptor is best-effort context enrichment.
        }
        return true;
    }
}
