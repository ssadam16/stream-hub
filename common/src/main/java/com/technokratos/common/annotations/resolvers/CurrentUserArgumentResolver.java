package com.technokratos.common.annotations.resolvers;

import com.technokratos.common.annotations.CurrentUser;
import com.technokratos.common.dto.CurrentUserInfo;
import com.technokratos.common.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                parameter.getParameterType().equals(CurrentUserInfo.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        boolean required = annotation != null && annotation.required();

        try {
            String emailHeader = request.getHeader(USER_EMAIL_HEADER);
            String rolesHeader = request.getHeader(USER_ROLES_HEADER);

            log.info("Extracting user from headers: email={}, roles={}", emailHeader, rolesHeader);

            if (required && (emailHeader == null || emailHeader.isEmpty())) {
                log.info("email header == null || email header is empty (required)");
                throw new IllegalStateException("User email header is missing but @CurrentUserInfo is required");
            }

            if (emailHeader == null || emailHeader.isEmpty()) {
                log.info("email header == null || email header is empty (non-required)");
                return null;
            }

            List<Role> roles = rolesHeader != null && !rolesHeader.isEmpty() ?
                    Arrays.stream(rolesHeader.split(","))
                            .map(String::trim)
                            .map(Role::valueOf)
                            .toList() :
                    List.of();

            log.info("Roles: {}", roles);

            return CurrentUserInfo.builder()
                    .email(emailHeader)
                    .roles(roles)
                    .build();

        } catch (Exception e) {
            log.error("Failed to resolve current user from headers", e);
            return null;
        }
    }
}
