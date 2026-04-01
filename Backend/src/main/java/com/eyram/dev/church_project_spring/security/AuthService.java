package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.security.dto.JwtResponse;
import com.eyram.dev.church_project_spring.security.dto.LoginRequest;
import com.eyram.dev.church_project_spring.security.jwt.JwtUtils;
import com.eyram.dev.church_project_spring.utils.exception.AccountDisabledException;
import com.eyram.dev.church_project_spring.utils.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public JwtResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
            String token = jwtUtils.generateToken(principal);
            return JwtResponse.from(token, principal);
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Identifiants incorrects");
        } catch (DisabledException ex) {
            throw new AccountDisabledException("Compte désactivé. Contactez un administrateur.");
        }
    }
}
