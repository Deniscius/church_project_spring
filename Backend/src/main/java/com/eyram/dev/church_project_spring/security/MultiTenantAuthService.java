package com.eyram.dev.church_project_spring.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.security.dto.LoginRequest;
import com.eyram.dev.church_project_spring.security.dto.MultiTenantLoginResponse;
import com.eyram.dev.church_project_spring.security.jwt.JwtUtils;
import com.eyram.dev.church_project_spring.utils.exception.AccountDisabledException;
import com.eyram.dev.church_project_spring.utils.exception.InvalidCredentialsException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service d'authentification multi-tenant.
 * Gère la connexion des utilisateurs avec accès à plusieurs paroisses.
 *
 * Bonnes pratiques appliquées:
 * - Logging de sécurité
 * - Isolation des données par tenant
 * - Validation des accès
 * - Gestion des exceptions personnalisées
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiTenantAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final ParoisseAccessRepository paroisseAccessRepository;

    /**
     * Authentifie un utilisateur et retourne ses paroisses d'accès.
     *
     * @param request les identifiants (username, password)
     * @return réponse contenant JWT et paroisses
     * @throws InvalidCredentialsException si les identifiants sont incorrects
     * @throws AccountDisabledException si le compte est désactivé
     */
    @Transactional(readOnly = true)
    public MultiTenantLoginResponse loginMultiTenant(LoginRequest request) {
        log.info("Authentication attempt for user: {}", request.username());

        try {
            // 1. Authentifier
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
            log.info("User {} authenticated successfully", request.username());

            // 2. Récupérer l'utilisateur complet
            User user = userRepository.findByUsernameAndStatusDelFalse(request.username())
                    .orElseThrow(() -> {
                        log.error("User not found after authentication: {}", request.username());
                        return new InvalidCredentialsException("Utilisateur non trouvé");
                    });

            // 3. Récupérer les accès paroissiaux actifs
            List<ParoisseAccess> paroisseAccesses = paroisseAccessRepository
                    .findByUserAndActiveTrueAndStatusDelFalse(user);

            log.info("User {} has access to {} paroisses", request.username(), paroisseAccesses.size());

            // 4. Générer JWT
            String token = jwtUtils.generateToken(principal);

            // 5. Mapper les accès
            List<MultiTenantLoginResponse.ParoisseAccessDto> paroissesDtos =
                    paroisseAccesses.stream()
                            .map(this::mapParoisseAccess)
                            .collect(Collectors.toList());

            // 6. Sélectionner la première paroisse par défaut
            MultiTenantLoginResponse.ParoisseAccessDto selectedParoisse =
                    paroissesDtos.isEmpty() ? null : paroissesDtos.get(0);

            // 7. Mapper l'utilisateur
            MultiTenantLoginResponse.UserInfoDto userDto = MultiTenantLoginResponse.UserInfoDto.builder()
                    .publicId(user.getPublicId())
                    .nom(user.getNom())
                    .prenom(user.getPrenom())
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .isGlobal(user.getIsGlobal())
                    .build();

            log.info("Login successful for user {} with selectedParoisse: {}",
                    request.username(), selectedParoisse != null ? selectedParoisse.getParoisseId() : "GLOBAL");

            return MultiTenantLoginResponse.builder()
                    .token(token)
                    .user(userDto)
                    .paroisses(paroissesDtos)
                    .selectedParoisse(selectedParoisse)
                    .build();

        } catch (BadCredentialsException ex) {
            log.warn("Failed authentication attempt for user: {} - Bad credentials", request.username());
            throw new InvalidCredentialsException("Identifiants incorrects");
        } catch (DisabledException ex) {
            log.warn("Authentication attempt for disabled account: {}", request.username());
            throw new AccountDisabledException("Compte désactivé. Contactez un administrateur.");
        } catch (Exception ex) {
            log.error("Unexpected error during authentication for user: {}", request.username(), ex);
            throw new InvalidCredentialsException("Erreur d'authentification");
        }
    }

    /**
     * Mappe un ParoisseAccess vers le DTO de réponse.
     */
    private MultiTenantLoginResponse.ParoisseAccessDto mapParoisseAccess(ParoisseAccess access) {
        return MultiTenantLoginResponse.ParoisseAccessDto.builder()
                .paroisseId(access.getParoisse().getPublicId())
                .paroisseNom(access.getParoisse().getNom())
                .adresse(access.getParoisse().getAdresse())
                .roleParoisse(access.getRoleParoisse().name())
                .active(access.getActive())
                .build();
    }

    /**
     * Vérifie si un utilisateur a accès à une paroisse.
     *
     * @param userId l'ID de l'utilisateur
     * @param paroisseId l'ID de la paroisse
     * @return true si accès
     */
    public boolean hasParoisseAccess(Long userId, Long paroisseId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        
        // Pour vérifier l'accès, on cherche juste si l'accès existe avec statusDel=false
        return user.getParoisseAccesses().stream()
                .anyMatch(pa -> pa.getParoisse().getId().equals(paroisseId) && !pa.getStatusDel());
    }

    /**
     * Vérifie si un utilisateur a un rôle spécifique dans une paroisse.
     *
     * @param userId l'ID de l'utilisateur
     * @param paroisseId l'ID de la paroisse
     * @param roleParoisseNames les noms des rôles acceptés
     * @return true si l'utilisateur a un des rôles
     */
    public boolean hasParoisseRole(Long userId, Long paroisseId, String... roleParoisseNames) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        
        return user.getParoisseAccesses().stream()
                .filter(pa -> pa.getParoisse().getId().equals(paroisseId) && !pa.getStatusDel())
                .anyMatch(pa -> {
                    for (String roleName : roleParoisseNames) {
                        if (pa.getRoleParoisse().name().equals(roleName)) {
                            return true;
                        }
                    }
                    return false;
                });
    }
}
