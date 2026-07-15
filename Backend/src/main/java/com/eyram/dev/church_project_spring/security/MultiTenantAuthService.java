package com.eyram.dev.church_project_spring.security;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eyram.dev.church_project_spring.entities.Paroisse;
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
 * Service d'authentification tenant-aware.
 *
 * Le modèle courant autorise :
 * - un utilisateur global sans paroisse sélectionnée ;
 * - un utilisateur local avec exactement une paroisse active.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiTenantAuthService {

    private static final String INVALID_LOCAL_ACCOUNT_MESSAGE =
            "Le compte doit être associé à une seule paroisse active. Contactez un administrateur.";

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final ParoisseAccessRepository paroisseAccessRepository;

    /**
     * Authentifie un utilisateur et retourne son contexte paroissial courant.
     *
     * @param request les identifiants (username, password)
     * @return réponse contenant JWT et contexte utilisateur
     * @throws InvalidCredentialsException si les identifiants sont incorrects
     * @throws AccountDisabledException si le compte est désactivé ou mal configuré
     */
    @Transactional(readOnly = true)
    public MultiTenantLoginResponse loginMultiTenant(LoginRequest request) {
        log.info("Authentication attempt for user: {}", request.username());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();

            User user = userRepository.findByUsernameAndStatusDelFalse(request.username())
                    .orElseThrow(() -> {
                        log.warn("User not found after successful authentication: {}", request.username());
                        return new InvalidCredentialsException("Identifiants incorrects");
                    });

            validateAuthenticatedUser(user);

            List<ParoisseAccess> eligibleAccesses = findEligibleParoisseAccesses(user);
            List<ParoisseAccess> responseAccesses = resolveResponseAccesses(user, eligibleAccesses);

            List<MultiTenantLoginResponse.ParoisseAccessDto> paroissesDtos = responseAccesses.stream()
                    .map(this::mapParoisseAccess)
                    .toList();

            MultiTenantLoginResponse.ParoisseAccessDto selectedParoisse =
                    paroissesDtos.isEmpty() ? null : paroissesDtos.get(0);

            String token = jwtUtils.generateToken(principal);

            MultiTenantLoginResponse.UserInfoDto userDto = MultiTenantLoginResponse.UserInfoDto.builder()
                    .publicId(user.getPublicId())
                    .nom(user.getNom())
                    .prenom(user.getPrenom())
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .isGlobal(user.getIsGlobal())
                    .build();

            log.info(
                    "Login successful for user {} with selectedParoisse: {}",
                    request.username(),
                    selectedParoisse != null ? selectedParoisse.getParoisseId() : "GLOBAL"
            );

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
        } catch (InvalidCredentialsException | AccountDisabledException ex) {
            throw ex;
        } catch (AuthenticationServiceException ex) {
            log.error("Authentication provider failure for user: {}", request.username(), ex);
            throw ex;
        }
    }

    private void validateAuthenticatedUser(User user) {
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AccountDisabledException("Compte désactivé. Contactez un administrateur.");
        }

        if (user.getRole() == null) {
            throw new AccountDisabledException(
                    "Le compte ne possède aucun rôle valide. Contactez un administrateur."
            );
        }
    }

    private List<ParoisseAccess> findEligibleParoisseAccesses(User user) {
        return paroisseAccessRepository
                .findByUserAndActiveTrueAndStatusDelFalse(user)
                .stream()
                .filter(access -> access != null && access.getRoleParoisse() != null)
                .filter(access -> isActiveParoisse(access.getParoisse()))
                .toList();
    }

    private List<ParoisseAccess> resolveResponseAccesses(
            User user,
            List<ParoisseAccess> eligibleAccesses
    ) {
        if (Boolean.TRUE.equals(user.getIsGlobal())) {
            if (!eligibleAccesses.isEmpty()) {
                log.warn(
                        "Global user {} has {} parish assignment(s); they are ignored during login",
                        user.getUsername(),
                        eligibleAccesses.size()
                );
            }
            return List.of();
        }

        if (eligibleAccesses.size() != 1) {
            log.warn(
                    "Local user {} has {} eligible active parish assignment(s)",
                    user.getUsername(),
                    eligibleAccesses.size()
            );
            throw new AccountDisabledException(INVALID_LOCAL_ACCOUNT_MESSAGE);
        }

        return eligibleAccesses;
    }

    private boolean isActiveParoisse(Paroisse paroisse) {
        return paroisse != null
                && Boolean.TRUE.equals(paroisse.getIsActive())
                && !Boolean.TRUE.equals(paroisse.getStatusDel());
    }

    private MultiTenantLoginResponse.ParoisseAccessDto mapParoisseAccess(ParoisseAccess access) {
        return MultiTenantLoginResponse.ParoisseAccessDto.builder()
                .paroisseId(access.getParoisse().getPublicId())
                .paroisseNom(access.getParoisse().getNom())
                .adresse(access.getParoisse().getAdresse())
                .roleParoisse(access.getRoleParoisse().name())
                .active(access.getActive())
                .build();
    }
}
