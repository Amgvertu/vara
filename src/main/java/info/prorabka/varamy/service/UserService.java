package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.request.AdminProfileUpdateRequest;
import info.prorabka.varamy.dto.request.ChangePasswordRequest;
import info.prorabka.varamy.dto.response.UserResponse;
import info.prorabka.varamy.entity.Ad;
import info.prorabka.varamy.entity.City;
import info.prorabka.varamy.entity.Profile;
import info.prorabka.varamy.entity.User;
import info.prorabka.varamy.exception.BadRequestException;
import info.prorabka.varamy.exception.ResourceNotFoundException;
import info.prorabka.varamy.mapper.UserMapper;
import info.prorabka.varamy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import info.prorabka.varamy.specification.UserSpecifications;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CityRepository cityRepository;
    private final ProfileRepository profileRepository;
    private final AdRepository adRepository;
    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserMessageRepository userMessageRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FileStorageService fileStorageService;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final FeedbackMessageRepository feedbackMessageRepository;
    private final AdminMessageRepository adminMessageRepository;
    private final UserNotificationSubscriptionRepository userNotificationSubscriptionRepository;
    private final UserStatRepository userStatRepository;


    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    public UserResponse getUserResponseById(UUID id) {
        return userMapper.toResponse(getUserById(id));
    }

    public User getCurrentUser(UUID userId) {
        return getUserById(userId);
    }

    public UserResponse getCurrentUserResponse(UUID userId) {
        return userMapper.toResponse(getCurrentUser(userId));
    }

    public Page<UserResponse> getUsers(String phone, User.UserRole role, User.UserStatus status, Pageable pageable) {
        return userRepository.findWithFilters(phone, role, status, pageable)
                .map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse updateUser(UUID id, String phone, User.UserRole role, User.UserStatus status, String password) {
        User user = getUserById(id);

        if (phone != null && !phone.equals(user.getPhone())) {
            if (userRepository.existsByPhone(phone)) {
                throw new BadRequestException("Телефон уже используется");
            }
            user.setPhone(phone);
        }

        if (role != null) {
            user.setRole(role);
        }

        if (status != null) {
            user.setStatus(status);
        }

        if (password != null) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Transactional
    public void deleteUser(UUID id, boolean hardDelete) {
        User user = getUserById(id);

        if (user.getProfile() != null) {
            String avatarUrl = user.getProfile().getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                fileStorageService.deleteFile(avatarUrl);
            }
            profileRepository.delete(user.getProfile());
        }

        if (hardDelete) {
            // 1. Удаляем все объявления пользователя (вместе с откликами, т.к. у Ad стоит cascade)
            List<Ad> ads = adRepository.findByAuthor(user, Pageable.unpaged()).getContent();
            if (!ads.isEmpty()) {
                adRepository.deleteAll(ads);
            }

            userMessageRepository.deleteByAdminMessageSenderId(user.getId());

            // 2. Удаляем уведомления
            notificationRepository.deleteByUserId(user.getId());

            // 3. Удаляем FCM-токены
            fcmTokenRepository.deleteByUserId(user.getId());

            // 4. Удаляем личные сообщения пользователя
            userMessageRepository.deleteByUserId(user.getId());

            // 5. Удаляем refresh-токены
            refreshTokenRepository.deleteByUser(user);

            // 6. Удаляем отправленные административные сообщения
            adminMessageRepository.deleteBySenderId(user.getId());

            // 7. Удаляем сообщения обратной связи
            feedbackMessageRepository.deleteByUserId(user.getId());

            // 8. Удаляем настройки уведомлений
            notificationSettingsRepository.deleteById(user.getId());

            // 9. Удаляем подписки на уведомления (НОВОЕ)
            userNotificationSubscriptionRepository.deleteByUserId(user.getId());

            // 10. Удаляем статистику пользователя (НОВОЕ, для надёжности)
            userStatRepository.deleteByUserId(user.getId());

            // 11. Удаляем профиль
            if (user.getProfile() != null) {
                profileRepository.delete(user.getProfile());
            }

            // 12. Удаляем самого пользователя
            userRepository.delete(user);
        } else {
            // Мягкое удаление – блокировка
            user.setStatus(User.UserStatus.BLOCKED);
            userRepository.save(user);
        }
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Неверный старый пароль");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void changePhone(UUID userId, String newPhone, String password) {
        User user = getUserById(userId);

        // Если передан пароль, проверяем его
        if (password != null && !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Неверный пароль");
        }

        if (userRepository.existsByPhone(newPhone)) {
            throw new BadRequestException("Телефон уже используется");
        }

        user.setPhone(newPhone);
        userRepository.save(user);
    }

    public boolean isPhoneExists(String phone) {
        return userRepository.existsByPhone(phone);
    }

    // UserService.java
    public List<User> findUsersForNewAdNotification(Long cityId, Integer type, Integer subType) {
        return userRepository.findUsersForNewAdNotification(cityId, type, subType);
    }

    public List<String> getAllTeams() {
        return userRepository.findDistinctTeamsByStatus(User.UserStatus.ACTIVE);
    }

    public Page<User> searchUsers(String query, Pageable pageable) {
        if (query == null || query.trim().length() < 2) {
            // если запрос слишком короткий, возвращаем пустую страницу или все?
            // По ТЗ – минимум 2 символа, но можно вернуть пустую страницу.
            return Page.empty(pageable);
        }
        return userRepository.searchUsers(query.trim(), pageable);
    }

    public Page<UserResponse> getUsersForAdmin(String search, List<String> roles,
                                               List<String> statuses, List<Long> cityIds,
                                               List<String> teams, Pageable pageable) {
        Specification<User> spec = Specification
                .where(UserSpecifications.searchBy(search))
                .and(UserSpecifications.hasRoles(roles))
                .and(UserSpecifications.hasStatuses(statuses))
                .and(UserSpecifications.hasCityIds(cityIds))
                .and(UserSpecifications.hasTeams(teams));

        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }

    // Метод для обновления профиля пользователя администратором
    @Transactional
    public UserResponse updateUserProfileByAdmin(UUID userId, AdminProfileUpdateRequest request) {
        User user = getUserById(userId);
        Profile profile = user.getProfile();
        if (profile == null) {
            profile = new Profile();
            profile.setUser(user);
            user.setProfile(profile);
        }
        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null) profile.setLastName(request.getLastName());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());
        if (request.getTeam() != null) profile.setTeam(request.getTeam());
        if (request.getPosition() != null) profile.setPosition(request.getPosition());
        if (request.getLevel() != null) profile.setLevel(request.getLevel());
        if (request.getNumber() != null) profile.setNumber(request.getNumber());
        if (request.getBirthDate() != null) profile.setBirthDate(request.getBirthDate());
        if (request.getHomeCityId() != null) {
            City city = cityRepository.findById(request.getHomeCityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Город не найден"));
            profile.setHomeCity(city);
        }
        profileRepository.save(profile);
        return userMapper.toResponse(user);
    }

}
