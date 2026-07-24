package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.request.AdvertisementRequest;
import info.prorabka.varamy.dto.request.AdvertisementStatusRequest;
import info.prorabka.varamy.dto.response.AdvertisementResponse;
import info.prorabka.varamy.dto.response.AdvertisementStatsResponse;
import info.prorabka.varamy.entity.Advertisement;
import info.prorabka.varamy.exception.BadRequestException;
import info.prorabka.varamy.exception.ResourceNotFoundException;
import info.prorabka.varamy.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final AdvertisementRepository advertisementRepository;

    // ========== СОЗДАНИЕ ==========

    @Transactional
    public AdvertisementResponse createAdvertisement(AdvertisementRequest request) {
        validateRequest(request);

        Advertisement ad = new Advertisement();
        ad.setAdvertiser(request.getAdvertiser());
        ad.setImageUrl(request.getImageUrl());
        ad.setLink(request.getLink());
        ad.setRefData(request.getRefData());
        ad.setType(request.getType());
        ad.setInterval(request.getInterval());
        ad.setPeriodDays(request.getPeriodDays());
        ad.setAllCities(request.getAllCities() != null && request.getAllCities());
        ad.setCityIds(request.getCityIds() != null ? request.getCityIds() : List.of());
        ad.setStartDate(LocalDateTime.now());
        ad.recalculateEndDate();
        ad.setStatus(Advertisement.AdvertisementStatus.ACTIVE);

        ad = advertisementRepository.save(ad);
        return toResponse(ad);
    }

    // ========== ОБНОВЛЕНИЕ ==========

    @Transactional
    public AdvertisementResponse updateAdvertisement(UUID id, AdvertisementRequest request) {
        Advertisement ad = getExisting(id);

        // Разрешаем обновление всех полей
        ad.setAdvertiser(request.getAdvertiser());
        ad.setImageUrl(request.getImageUrl());
        ad.setLink(request.getLink());
        ad.setRefData(request.getRefData());
        ad.setType(request.getType());
        ad.setInterval(request.getInterval());
        ad.setPeriodDays(request.getPeriodDays());
        ad.setAllCities(request.getAllCities() != null && request.getAllCities());
        ad.setCityIds(request.getCityIds() != null ? request.getCityIds() : List.of());
        // Пересчитываем endDate только если изменился startDate или periodDays
        // Если startDate не менялся, то оставляем прежний startDate и пересчитываем endDate
        ad.recalculateEndDate();
        ad.setUpdatedAt(LocalDateTime.now());

        ad = advertisementRepository.save(ad);
        return toResponse(ad);
    }

    // ========== СМЕНА СТАТУСА ==========

    @Transactional
    public void changeStatus(UUID id, AdvertisementStatusRequest request) {
        Advertisement ad = getExisting(id);
        Advertisement.AdvertisementStatus newStatus = request.getStatus();
        // Нельзя перевести в EXPIRED или DELETED через этот метод (они устанавливаются автоматически)
        if (newStatus == Advertisement.AdvertisementStatus.EXPIRED || newStatus == Advertisement.AdvertisementStatus.DELETED) {
            throw new BadRequestException("Статус EXPIRED или DELETED нельзя установить вручную");
        }
        ad.setStatus(newStatus);
        ad.setUpdatedAt(LocalDateTime.now());
        advertisementRepository.save(ad);
    }

    // ========== УДАЛЕНИЕ (МЯГКОЕ) ==========

    @Transactional
    public void deleteAdvertisement(UUID id) {
        Advertisement ad = getExisting(id);
        ad.setStatus(Advertisement.AdvertisementStatus.DELETED);
        ad.setUpdatedAt(LocalDateTime.now());
        advertisementRepository.save(ad);
    }

    // ========== ПОЛУЧЕНИЕ АКТИВНЫХ РЕКЛАМ ДЛЯ КЛИЕНТА ==========

    public List<AdvertisementResponse> getActiveAdvertisements(Integer type, Long cityId) {
        LocalDateTime now = LocalDateTime.now();
        List<Advertisement> ads = advertisementRepository.findActiveByTypeAndCity(type, cityId, now);
        return ads.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ========== ПОЛУЧЕНИЕ ПО ID ==========

    public AdvertisementResponse getAdvertisementById(UUID id) {
        Advertisement ad = getExisting(id);
        return toResponse(ad);
    }

    // ========== АДМИНИСТРАТИВНЫЙ СПИСОК (С ФИЛЬТРАМИ) ==========

    public Page<AdvertisementResponse> getAdminAdvertisements(
            List<Advertisement.AdvertisementStatus> statuses,
            String advertiser,
            List<Long> cityIds,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            LocalDateTime endDateFrom,
            LocalDateTime endDateTo,
            Pageable pageable) {

        Specification<Advertisement> spec = Specification.where(null);

        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("status").in(statuses));
        }
        if (advertiser != null && !advertiser.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("advertiser")), "%" + advertiser.toLowerCase() + "%"));
        }
        if (cityIds != null && !cityIds.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.isTrue(root.get("allCities")),
                            cb.and(
                                    cb.isNotEmpty(root.get("cityIds")),
                                    root.get("cityIds").in(cityIds)
                            )
                    ));
        }
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
        }
        if (dateTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
        }
        if (endDateFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("endDate"), endDateFrom));
        }
        if (endDateTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("endDate"), endDateTo));
        }

        Page<Advertisement> page = advertisementRepository.findAll(spec, pageable);
        return page.map(this::toResponse);
    }

    // ========== СТАТИСТИКА ==========

    public AdvertisementStatsResponse getStatistics(List<Long> cityIds, LocalDateTime dateFrom, LocalDateTime dateTo) {
        // Простейшая реализация – считаем количество по статусам
        // Можно расширить фильтрацию по городам и датам
        Specification<Advertisement> spec = Specification.where(null);
        if (cityIds != null && !cityIds.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.isTrue(root.get("allCities")),
                            cb.and(
                                    cb.isNotEmpty(root.get("cityIds")),
                                    root.get("cityIds").in(cityIds)
                            )
                    ));
        }
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
        }
        if (dateTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
        }

        List<Advertisement> all = advertisementRepository.findAll(spec);
        long total = all.size();
        long active = all.stream().filter(a -> a.getStatus() == Advertisement.AdvertisementStatus.ACTIVE).count();
        long paused = all.stream().filter(a -> a.getStatus() == Advertisement.AdvertisementStatus.PAUSED).count();
        long expired = all.stream().filter(a -> a.getStatus() == Advertisement.AdvertisementStatus.EXPIRED).count();
        long deleted = all.stream().filter(a -> a.getStatus() == Advertisement.AdvertisementStatus.DELETED).count();

        return new AdvertisementStatsResponse(total, active, paused, expired, deleted);
    }

    // ========== ФОНОВАЯ ЗАДАЧА: АВТОМАТИЧЕСКОЕ ИСТЕЧЕНИЕ СРОКА ==========

    @Scheduled(cron = "0 */20 * * * *")  // каждые 20 минут
    @Transactional
    public void expireAdvertisements() {
        LocalDateTime now = LocalDateTime.now();
        List<Advertisement> expired = advertisementRepository.findExpiredActive(now);
        if (!expired.isEmpty()) {
            log.info("Найдено {} истекших реклам, меняем статус на EXPIRED", expired.size());
            for (Advertisement ad : expired) {
                ad.setStatus(Advertisement.AdvertisementStatus.EXPIRED);
                ad.setUpdatedAt(now);
            }
            advertisementRepository.saveAll(expired);
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private Advertisement getExisting(UUID id) {
        return advertisementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Реклама не найдена"));
    }

    private void validateRequest(AdvertisementRequest request) {
        if (request.getType() == 1 && request.getInterval() == null) {
            throw new BadRequestException("Для типа 1 (в ленте) необходимо указать интервал (5-10)");
        }
        if (request.getType() == 1 && (request.getInterval() < 5 || request.getInterval() > 10)) {
            throw new BadRequestException("Интервал должен быть от 5 до 10");
        }
        if (request.getType() == 2 && request.getInterval() != null) {
            throw new BadRequestException("Для типа 2 (в диалоге) интервал не указывается");
        }
        // Проверка городов
        if ((request.getAllCities() == null || !request.getAllCities()) && (request.getCityIds() == null || request.getCityIds().isEmpty())) {
            throw new BadRequestException("Необходимо указать хотя бы один город или выбрать 'Все города'");
        }
    }

    private AdvertisementResponse toResponse(Advertisement ad) {
        return AdvertisementResponse.builder()
                .id(ad.getId())
                .advertiser(ad.getAdvertiser())
                .imageUrl(ad.getImageUrl())
                .link(ad.getLink())
                .refData(ad.getRefData())
                .type(ad.getType())
                .interval(ad.getInterval())
                .periodDays(ad.getPeriodDays())
                .allCities(ad.isAllCities())
                .cityIds(ad.getCityIds())
                .status(ad.getStatus())
                .startDate(ad.getStartDate())
                .endDate(ad.getEndDate())
                .createdAt(ad.getCreatedAt())
                .updatedAt(ad.getUpdatedAt())
                .build();
    }
}
