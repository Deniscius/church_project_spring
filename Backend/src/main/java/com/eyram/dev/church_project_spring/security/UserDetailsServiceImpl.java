package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserTenantResolver userTenantResolver;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login)
            throws UsernameNotFoundException {

        String normalized = login == null
                ? ""
                : login.strip().toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByUsernameIgnoreCaseAndStatusDelFalse(normalized)
                .or(() -> normalized.contains("@")
                        ? userRepository.findByEmailIgnoreCaseAndStatusDelFalse(normalized)
                        : java.util.Optional.empty())
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Utilisateur introuvable : " + normalized
                        )
                );

        Long tenantId = userTenantResolver.resolveTenantId(user);

        return UserDetailsImpl.build(user, tenantId);
    }
}
