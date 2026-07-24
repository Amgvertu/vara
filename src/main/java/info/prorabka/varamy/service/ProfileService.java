package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.request.ProfileRequest;
import info.prorabka.varamy.dto.response.ProfileResponse;
import info.prorabka.varamy.entity.City;
import info.prorabka.varamy.entity.Country;
import info.prorabka.varamy.entity.Profile;
import info.prorabka.varamy.entity.Region;
import info.prorabka.varamy.exception.BadRequestException;
import info.prorabka.varamy.exception.ResourceNotFoundException;
import info.prorabka.varamy.mapper.ProfileMapper;
import info.prorabka.varamy.repository.CityRepository;
import info.prorabka.varamy.repository.CountryRepository;
import info.prorabka.varamy.repository.ProfileRepository;
import info.prorabka.varamy.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final CountryRepository countryRepository;
    private final RegionRepository regionRepository;
    private final CityRepository cityRepository;
    private final ProfileMapper profileMapper;
    private final FileStorageService fileStorageService;
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5 MB

    public ProfileResponse getProfileById(UUID id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль не найден"));
        return profileMapper.toResponse(profile);
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, ProfileRequest request) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль не найден"));

        if (request.getHomeCountryId() != null) {
            Country country = countryRepository.findById(request.getHomeCountryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Страна не найдена"));
            profile.setHomeCountry(country);
        }

        if (request.getHomeRegionId() != null) {
            Region region = regionRepository.findById(request.getHomeRegionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Регион не найден"));
            profile.setHomeRegion(region);
        }

        if (request.getHomeCityId() != null) {
            City city = cityRepository.findById(request.getHomeCityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Город не найден"));
            profile.setHomeCity(city);
        }

        profileMapper.updateProfile(profile, request);
        profile = profileRepository.save(profile);

        return profileMapper.toResponse(profile);
    }

    @Transactional
    public String uploadAvatar(UUID userId, MultipartFile file) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль не найден"));
        // Проверка размера
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BadRequestException("Размер аватара не должен превышать 5 МБ");
        }

        // Удаляем старый аватар, если он есть
        String oldAvatarUrl = profile.getAvatarUrl();
        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
            fileStorageService.deleteFile(oldAvatarUrl);
        }

        String avatarUrl = fileStorageService.storeFile(file, "avatars");
        profile.setAvatarUrl(avatarUrl);
        profileRepository.save(profile);

        return avatarUrl;
    }

    @Transactional
    public void deleteAvatar(UUID userId) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль не найден"));

        String avatarUrl = profile.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            fileStorageService.deleteFile(avatarUrl);
            profile.setAvatarUrl(null);
            profileRepository.save(profile);
        } else {
            throw new ResourceNotFoundException("Аватар не найден");
        }
    }
}
