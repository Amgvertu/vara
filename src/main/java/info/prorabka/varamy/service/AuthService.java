package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.request.LoginRequest;
import info.prorabka.varamy.dto.request.RefreshTokenRequest;
import info.prorabka.varamy.dto.request.RegisterRequest;
import info.prorabka.varamy.dto.response.AuthResponse;
import info.prorabka.varamy.dto.response.UserResponse;
import info.prorabka.varamy.entity.*;
import info.prorabka.varamy.event.UserRegisteredEvent;
import info.prorabka.varamy.exception.BadRequestException;
import info.prorabka.varamy.exception.ResourceNotFoundException;
import info.prorabka.varamy.exception.UnauthorizedException;
import info.prorabka.varamy.mapper.UserMapper;
import info.prorabka.varamy.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;
    private final RegionRepository regionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    @Autowired private ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Пользователь с таким телефоном уже существует");
        }

        Country country = null;
        Region region = null;
        City city = null;

        if (request.getCountryId() != null) {
            country = countryRepository.findById(request.getCountryId())
                    .orElseThrow(() -> new BadRequestException("Страна не найдена"));
        }

        if (request.getRegionId() != null) {
            region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new BadRequestException("Регион не найден"));
        }

        if (request.getCityId() != null) {
            city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new BadRequestException("Город не найден"));
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.UserRole.USER);
        user.setSubrole(User.UserSubrole.PLAYER);
        user.setStatus(User.UserStatus.ACTIVE);

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setHomeCountry(country);
        profile.setHomeRegion(region);
        profile.setHomeCity(city);
        user.setProfile(profile);

        user = userRepository.save(user);

        applicationEventPublisher.publishEvent(new UserRegisteredEvent(user));

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = jwtService.generateRefreshToken(
                user,
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr()
        );

        UserResponse userResponse = userMapper.toResponse(user);

        return new AuthResponse(accessToken, refreshToken.getToken(), userResponse);
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userRepository.findByPhone(request.getPhone())
                                .orElseThrow(() -> new UnauthorizedException("Неверный телефон или пароль"))
                                .getId().toString(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new UnauthorizedException("Неверный телефон или пароль"));

        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = jwtService.generateRefreshToken(
                user,
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr()
        );

        UserResponse userResponse = userMapper.toResponse(user);

        return new AuthResponse(accessToken, refreshToken.getToken(), userResponse);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken, HttpServletRequest httpRequest) {
        RefreshToken token = jwtService.verifyRefreshToken(refreshToken);

        String accessToken = jwtService.generateAccessToken(token.getUser());
        RefreshToken newRefreshToken = jwtService.generateRefreshToken(
                token.getUser(),
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr()
        );

        UserResponse userResponse = userMapper.toResponse(token.getUser());

        return new AuthResponse(accessToken, newRefreshToken.getToken(), userResponse);
    }

    @Transactional
    public void logout(String refreshToken) {
        jwtService.deleteRefreshToken(refreshToken);
    }

    @Transactional
    public void resetPassword(String phone, String newPassword) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Опционально: удалить все refresh токены пользователя
        // refreshTokenRepository.deleteByUser(user);
    }
}