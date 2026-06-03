package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.AuthDto;
import com.griddynamics.forge.market_presence_service.entity.CandidateProfile;
import com.griddynamics.forge.market_presence_service.entity.User;
import com.griddynamics.forge.market_presence_service.exception.ConflictException;
import com.griddynamics.forge.market_presence_service.repository.CandidateProfileRepository;
import com.griddynamics.forge.market_presence_service.repository.UserRepository;
import com.griddynamics.forge.market_presence_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(User.UserRole.CANDIDATE)
            .active(true)
            .build();
        userRepository.save(user);

        CandidateProfile profile = CandidateProfile.builder()
            .user(user)
            .fullName(request.getName())
            .build();
        profileRepository.save(profile);

        String token = jwtUtil.generateToken(user);
        return AuthDto.AuthResponse.builder()
            .token(token)
            .email(user.getEmail())
            .role(user.getRole().name())
            .userId(user.getId().toString())
            .name(request.getName())
            .build();
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user);
        String name = profileRepository.findByUser(user)
            .map(CandidateProfile::getFullName).orElse("");

        return AuthDto.AuthResponse.builder()
            .token(token)
            .email(user.getEmail())
            .role(user.getRole().name())
            .userId(user.getId().toString())
            .name(name)
            .build();
    }
}
