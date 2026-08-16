package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.request.AdFilterRequest;
import info.prorabka.varamy.dto.request.AdRequest;
import info.prorabka.varamy.dto.response.AdResponse;
import info.prorabka.varamy.dto.response.DuplicateAdResponse;
import info.prorabka.varamy.entity.*;
import info.prorabka.varamy.event.AdCreatedEvent;
import info.prorabka.varamy.exception.BadRequestException;
import info.prorabka.varamy.exception.ResourceNotFoundException;
import info.prorabka.varamy.exception.UnauthorizedException;
import info.prorabka.varamy.mapper.AdMapper;
import info.prorabka.varamy.repository.AdRepository;
import info.prorabka.varamy.repository.CityRepository;
import info.prorabka.varamy.repository.RinkRepository;
import info.prorabka.varamy.specification.AdSpecifications;
import lombok.Builder;
import lombok.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import info.prorabka.varamy.specification.AdAdminSpecifications;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdService {

    private final AdRepository adRepository;
    private final CityRepository cityRepository;
    private final RinkRepository rinkRepository;
    private final AdMapper adMapper;
    private final UserService userService;
    private final NotificationService notificationService;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    // ============= ПУБЛИЧНЫЕ МЕТОДЫ (ACTIVE и FILLED) =============

    // ============= ПУБЛИЧНЫЕ МЕТОДЫ (ACTIVE и FILLED) =============

    public Page<AdResponse> getAds(Long cityId, Integer type, Integer subType,
                                   List<String> level, Pageable pageable) {
        if (cityId == null) {
            throw new BadRequestException("Параметр cityId обязателен");
        }
        return adRepository.findActiveAndFilledAds(
                        cityId, type, subType,
                        List.of(Ad.AdStatus.ACTIVE, Ad.AdStatus.FILLED),
                        level, null, pageable)
                .map(adMapper::toResponse);
    }

    public Page<AdResponse> getAllActiveAds(Integer type, Integer subType, List<String> level, Pageable pageable) {
        return adRepository.findAllActiveAndFilledPublic(
                        type, subType,
                        List.of(Ad.AdStatus.ACTIVE, Ad.AdStatus.FILLED),
                        level, pageable)
                .map(adMapper::toResponse);
    }

    public Page<AdResponse> getFilteredAds(AdFilterRequest filter, Pageable pageable) {
        Specification<Ad> spec = Specification.where(AdSpecifications.hasStatuses())
                .and(AdSpecifications.hasCityId(filter.getCityId()))
                .and(AdSpecifications.hasType(filter.getType()))
                .and(AdSpecifications.hasSubType(filter.getSubType()))
                .and(AdSpecifications.hasRole(filter.getRole()))
                .and(AdSpecifications.hasLevels(filter.getLevel()))
                .and(AdSpecifications.dateBetween(filter.getDateFrom(), filter.getDateTo()))
                .and(AdSpecifications.timeBetween(filter.getTimeFrom(), filter.getTimeTo()))
                .and(AdSpecifications.hasRinkIds(filter.getRinkIds()));

        return adRepository.findAll(spec, pageable).map(adMapper::toResponse);
    }

    // ============= ПОЛУЧЕНИЕ ОБЪЯВЛЕНИЯ ПО ID =============

    public AdResponse getAdById(UUID id) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление не найдено"));
        return adMapper.toResponse(ad);
    }

    // ============= МЕТОДЫ ДЛЯ АВТОРА =============

    public Page<AdResponse> getMyAds(UUID userId, Pageable pageable) {
        User user = userService.getUserById(userId);
        return adRepository.findByAuthor(user, pageable)
                .map(adMapper::toResponse);
    }

    // ============= АДМИНИСТРАТИВНЫЕ МЕТОДЫ =============

    public Page<AdResponse> getAdsAdmin(List<Long> cityIds, List<Integer> types,
                                        List<Integer> subTypes, List<Ad.AdStatus> statuses,
                                        List<String> levels, List<UUID> authorIds,
                                        List<Long> rinkIds, String search,
                                        Pageable pageable) {
        Specification<Ad> spec = Specification
                .where(AdAdminSpecifications.hasCityIds(cityIds))
                .and(AdAdminSpecifications.hasTypes(types))
                .and(AdAdminSpecifications.hasSubTypes(subTypes))
                .and(AdAdminSpecifications.hasStatuses(statuses))
                .and(AdAdminSpecifications.hasLevels(levels))
                .and(AdAdminSpecifications.hasAuthorIds(authorIds))
                .and(AdAdminSpecifications.hasRinkIds(rinkIds))
                .and(AdAdminSpecifications.searchBy(search));

        return adRepository.findAll(spec, pageable).map(adMapper::toResponse);
    }

    // ============= МЕТОДЫ ДЛЯ МОДЕРАЦИИ =============

    public Page<AdResponse> getAdsForModeration(Pageable pageable) {
        return adRepository.findByStatus(Ad.AdStatus.MODERATION, pageable)
                .map(adMapper::toResponse);
    }

    // ============= СОЗДАНИЕ ОБЪЯВЛЕНИЯ =============

    @Transactional
    public AdResponse createAd(UUID authorId, AdRequest request) {
        User author = userService.getUserById(authorId);
        Profile profile = author.getProfile();

        validateAdRequest(request);

        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("Город не найден"));

        Ad ad = adMapper.toEntity(request, city);
        ad.setAuthor(author);
        ad.setCity(city);
        ad.setStatus(Ad.AdStatus.ACTIVE);

        String fullName = (profile.getFirstName() != null ? profile.getFirstName() : "") +
                (profile.getLastName() != null ? " " + profile.getLastName() : "");
        ad.setContactName(fullName.trim().isEmpty() ? author.getPhone() : fullName.trim());
        ad.setContactPhone(author.getPhone());

        if (request.getTeam() == null && profile.getTeam() != null) {
            ad.setTeam(profile.getTeam());
        }

        if (request.getLevel() != null && !request.getLevel().isEmpty()) {
            List<String> validLevels = List.of("A", "B", "C", "D", "E", "F", "G", "H");
            for (String level : request.getLevel()) {
                if (!validLevels.contains(level)) {
                    throw new BadRequestException("Неверный уровень: " + level + ". Допустимые значения: A-H");
                }
            }
            ad.setLevels(request.getLevel());
        }

        if (request.getType() == 1) {
            if (request.getSubType() == 1) {
                ad.setGoaliesCount(request.getGoaliesCount());
            } else if (request.getSubType() == 2) {
                ad.setDefendersCount(request.getDefendersCount());
                ad.setForwardsCount(request.getForwardsCount());
            }
        }

        ad.setAcceptedGoaliesCount(0);
        ad.setAcceptedDefendersCount(0);
        ad.setAcceptedForwardsCount(0);
        ad.setAcceptedResponsesCount(0);

        if (request.getEndTime() == null && !requiresEndTime(request.getType(), request.getSubType())) {
            ad.setEndTime(request.getStartTime());
        } else {
            ad.setEndTime(request.getEndTime());
        }

        // ---- Добавление катков (если переданы) ----
        if (request.getRinkIds() != null && !request.getRinkIds().isEmpty()) {
            Set<AdRink> adRinks = new HashSet<>();
            for (Long rinkId : request.getRinkIds()) {
                Rink rink = rinkRepository.findById(rinkId)
                        .orElseThrow(() -> new ResourceNotFoundException("Каток не найден: " + rinkId));
                AdRink adRink = new AdRink();
                AdRink.AdRinkId adRinkId = new AdRink.AdRinkId();  // создаём ключ
                adRink.setId(adRinkId);                             // устанавливаем ключ
                adRink.setAd(ad);
                adRink.setRink(rink);
                adRinks.add(adRink);
            }
            ad.setAdRinks(adRinks);
        }

        ad = adRepository.save(ad);
        applicationEventPublisher.publishEvent(new AdCreatedEvent(ad));

        // 1. Уведомления подписчикам (внутри транзакции) – работает как раньше
        notificationService.onNewAdCreated(ad);

        // 2. Отложенная отправка AD_CREATED с правильными rinkIds
        final Ad savedAd = ad;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Загружаем свежую версию с уже сохранёнными катками
                Ad freshAd = adRepository.findById(savedAd.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Объявление не найдено"));
                // Лог для проверки
                log.info("AD_CREATED after commit: adId={}, rinkIds={}",
                        freshAd.getId(),
                        freshAd.getAdRinks().stream()
                                .map(ar -> ar.getRink().getId())
                                .collect(Collectors.toList()));
                notificationService.sendAdCreated(freshAd);
            }
        });

        return adMapper.toResponse(ad);
    }

    // ============= ОБНОВЛЕНИЕ ОБЪЯВЛЕНИЯ =============

    @Transactional
    public AdResponse updateAd(UUID adId, UUID userId, AdRequest request, boolean isAdmin) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление не найдено"));

        // Проверка прав
        if (!isAdmin && !ad.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("Нет прав на редактирование этого объявления");
        }

        // --- 1. Обработка изменения статуса (архивация / возврат из архива) ---
        if (request.getStatus() != null) {
            Ad.AdStatus newStatus = request.getStatus();
            Ad.AdStatus currentStatus = ad.getStatus();

            // Только автор или админ могут менять статус
            if (!isAdmin && !ad.getAuthor().getId().equals(userId)) {
                throw new BadRequestException("Недостаточно прав для смены статуса");
            }

            // Проверка допустимых переходов
            if (newStatus == Ad.AdStatus.ARCHIVED) {
                // Архивировать можно всегда
                ad.setStatus(Ad.AdStatus.ARCHIVED);
            } else if (newStatus == Ad.AdStatus.ACTIVE && currentStatus == Ad.AdStatus.ARCHIVED) {
                // Возврат из архива – только если срок не истёк
                if (ad.getEndTime() != null && LocalDateTime.now().isAfter(ad.getEndTime())) {
                    throw new BadRequestException("Нельзя вернуть объявление из архива – срок истёк");
                }
                ad.setStatus(Ad.AdStatus.ACTIVE);
            } else if (isAdmin) {
                // Администратор может установить любой статус
                ad.setStatus(newStatus);
            } else {
                throw new BadRequestException("Недопустимый статус: " + newStatus);
            }

            ad = adRepository.save(ad);
            notificationService.sendAdStatusUpdate(ad);   // отправит WebSocket‑событие и уведомление
            return adMapper.toResponse(ad);
        }

        // --- 2. Частичное обновление данных объявления ---
        City city = ad.getCity();
        if (request.getCityId() != null) {
            city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Город не найден"));
            ad.setCity(city);
        }

        // Применяем изменения через MapStruct (игнорирует null-поля)
        adMapper.updateAd(ad, request, city);

        // Валидация только если изменились тип или подтип
        if (request.getType() != null || request.getSubType() != null) {
            validateAdRequest(request);
        }

        // Уровни
        if (request.getLevel() != null) {
            if (!request.getLevel().isEmpty()) {
                List<String> validLevels = List.of("A", "B", "C", "D", "E", "F", "G", "H");
                for (String level : request.getLevel()) {
                    if (!validLevels.contains(level)) {
                        throw new BadRequestException("Неверный уровень: " + level);
                    }
                }
                ad.setLevels(request.getLevel());
            } else {
                ad.setLevels(null);
            }
        }

        // Катки
        if (request.getRinkIds() != null) {
            ad.getAdRinks().clear();
            if (!request.getRinkIds().isEmpty()) {
                Set<AdRink> adRinks = new HashSet<>();
                for (Long rinkId : request.getRinkIds()) {
                    Rink rink = rinkRepository.findById(rinkId)
                            .orElseThrow(() -> new ResourceNotFoundException("Каток не найден"));
                    AdRink adRink = new AdRink();
                    AdRink.AdRinkId adRinkId = new AdRink.AdRinkId();
                    adRink.setId(adRinkId);
                    adRink.setAd(ad);
                    adRink.setRink(rink);
                    adRinks.add(adRink);
                }
                ad.setAdRinks(adRinks);
            }
        }

        // Дата окончания
        if (request.getEndTime() == null && !requiresEndTime(
                request.getType() != null ? request.getType() : ad.getType(),
                request.getSubType() != null ? request.getSubType() : ad.getSubType())) {
            ad.setEndTime(request.getStartTime());
        }

        ad = adRepository.save(ad);
        notificationService.sendAdUpdated(ad);
        return adMapper.toResponse(ad);
    }

    // ============= УДАЛЕНИЕ =============

    @Transactional
    public void deleteAd(UUID adId, UUID userId, boolean isAdmin) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление не найдено"));

        if (!isAdmin && !ad.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("Нет прав на удаление этого объявления");
        }

        adRepository.delete(ad);

        // Отправка события AD_DELETED после успешного коммита транзакции
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    notificationService.sendAdDeleted(adId);
                } catch (Exception e) {
                    log.error("Ошибка отправки AD_DELETED для объявления {}", adId, e);
                }
            }
        });
    }

    // ============= МОДЕРАЦИЯ =============

    @Transactional
    public AdResponse moderateAd(UUID adId, boolean approve) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление не найдено"));

        if (approve) {
            ad.setStatus(Ad.AdStatus.ACTIVE);
        } else {
            ad.setStatus(Ad.AdStatus.ARCHIVED);
        }

        ad = adRepository.save(ad);
        notificationService.sendAdStatusUpdate(ad);
        return adMapper.toResponse(ad);
    }

    // ============= АВТОМАТИЧЕСКАЯ АРХИВАЦИЯ =============

    @Scheduled(cron = "0 */20 * * * *")
    @Transactional
    public void archiveExpiredAds() {
        LocalDateTime now = LocalDateTime.now();
        List<Ad> expiredAds = adRepository.findByEndTimeBeforeAndStatusNot(now, Ad.AdStatus.ARCHIVED);

        int archivedCount = 0;
        for (Ad ad : expiredAds) {
            if (ad.getEndTime().isBefore(now)) {
                log.info("Архивация объявления id={}, endTime={}, status={}",
                        ad.getId(), ad.getEndTime(), ad.getStatus());
                ad.setStatus(Ad.AdStatus.ARCHIVED);
                notificationService.sendAdStatusUpdate(ad);
                archivedCount++;
            }
        }

        if (archivedCount > 0) {
            adRepository.saveAll(expiredAds);
            //log.info("Архивировано {} объявлений", archivedCount);
        }
    }

    // ============= ОЧИСТКА СТАРЫХ АРХИВНЫХ ОБЪЯВЛЕНИЙ =============

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void cleanupOldArchivedAds() {
        LocalDateTime fiveMonthsAgo = LocalDateTime.now().minusMonths(5);

        List<Ad> oldArchivedAds = adRepository.findByStatusAndCreatedAtBefore(
                Ad.AdStatus.ARCHIVED, fiveMonthsAgo);

        int deletedCount = oldArchivedAds.size();

        if (deletedCount > 0) {
            log.info("Удаление {} старых архивных объявлений (созданы до {})",
                    deletedCount, fiveMonthsAgo);
            adRepository.deleteAll(oldArchivedAds);

            long countAfter = adRepository.countByStatus(Ad.AdStatus.ARCHIVED);
            log.info("Очистка завершена. Удалено {} объявлений. Осталось архивных: {}",
                    deletedCount, countAfter);
        } else {
            log.debug("Старых архивных объявлений для удаления не найдено");
        }
    }

    // ============= СТАТИСТИКА =============

    public AdStatistics getAdStatistics() {
        return AdStatistics.builder()
                .totalActive(adRepository.countByStatus(Ad.AdStatus.ACTIVE))
                .totalModeration(adRepository.countByStatus(Ad.AdStatus.MODERATION))
                .totalFilled(adRepository.countByStatus(Ad.AdStatus.FILLED))
                .totalArchived(adRepository.countByStatus(Ad.AdStatus.ARCHIVED))
                .build();
    }

    // ============= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ВАЛИДАЦИИ =============

    private void validateAdRequest(AdRequest request) {
        if (request.getType() == null) {
            return;
        }
        if (request.getSubType() == null) {
            throw new BadRequestException("Подтип объявления (subType) обязателен");
        }

        switch (request.getType()) {
            case 1:
                if (request.getSubType() != 1 && request.getSubType() != 2) {
                    throw new BadRequestException("Для типа 1 допустимы подтипы 1 (вратарь) или 2 (полевой)");
                }
                break;
            case 2:
                if (request.getSubType() != 1 && request.getSubType() != 2) {
                    throw new BadRequestException("Для типа 2 допустимы подтипы 1 (вратарь) или 2 (полевой)");
                }
                break;
            case 3:
                if (request.getSubType() != 1 && request.getSubType() != 2) {
                    throw new BadRequestException("Для типа 3 допустимы подтипы 1 (ищу) или 2 (предлагаю)");
                }
                break;
            case 4:
                if (request.getSubType() < 1 || request.getSubType() > 4) {
                    throw new BadRequestException("Для типа 4 допустимы подтипы 1-4 (судья, фотограф, медик, тренер)");
                }
                break;
            default:
                throw new BadRequestException("Неверный тип объявления");
        }

        if (isSingleRinkType(request.getType(), request.getSubType())) {
            if (request.getRinkIds() != null && request.getRinkIds().size() > 1) {
                throw new BadRequestException("Для этого типа объявления можно выбрать только один ЛДС");
            }
        }

        if (requiresEndTime(request.getType(), request.getSubType())) {
            if (request.getEndTime() == null) {
                throw new BadRequestException("Для этого типа объявления необходимо указать время окончания");
            }
            if (request.getStartTime() != null && request.getEndTime().isBefore(request.getStartTime())) {
                throw new BadRequestException("Время окончания не может быть раньше времени начала");
            }
        } else {
            if (request.getEndTime() != null) {
                throw new BadRequestException("Для этого типа объявления время окончания не требуется");
            }
        }

        if (request.getType() == 1) {
            if (request.getSubType() == 1) {
                if (request.getGoaliesCount() == null) {
                    throw new BadRequestException("Не указано количество вратарей");
                }
                if (request.getGoaliesCount() != 1 && request.getGoaliesCount() != 2) {
                    throw new BadRequestException("Количество вратарей может быть 1 или 2");
                }
            } else if (request.getSubType() == 2) {
                // ---------- ИСПРАВЛЕННАЯ ВАЛИДАЦИЯ ----------
                Integer def = request.getDefendersCount();
                Integer fwd = request.getForwardsCount();

                boolean hasDefenders = def != null && def > 0;
                boolean hasForwards = fwd != null && fwd > 0;

                if (!hasDefenders && !hasForwards) {
                    throw new BadRequestException("Должен быть указан хотя бы один игрок (защитник или нападающий)");
                }

                if (def != null && def < 0) {
                    throw new BadRequestException("Количество защитников не может быть отрицательным");
                }
                if (fwd != null && fwd < 0) {
                    throw new BadRequestException("Количество нападающих не может быть отрицательным");
                }
            }
        } else {
            if (request.getGoaliesCount() != null || request.getDefendersCount() != null || request.getForwardsCount() != null) {
                throw new BadRequestException("Для этого типа объявления количество игроков не указывается");
            }
        }

        if (!requiresLevel(request.getType(), request.getSubType())) {
            if (request.getLevel() != null && !request.getLevel().isEmpty()) {
                throw new BadRequestException("Для этого типа объявления уровни не указываются");
            }
        }
    }

    // AdService.java
    public List<DuplicateAdResponse> checkDuplicate(AdRequest request) {
        if (!shouldCheckDuplicate(request.getType(), request.getSubType())) {
            return List.of();
        }

        // Время в минутах для проверки (±30 минут)
        long timeThresholdMinutes = 30;
        LocalDateTime startTimeMinus = request.getStartTime().minusMinutes(timeThresholdMinutes);
        LocalDateTime startTimePlus = request.getStartTime().plusMinutes(timeThresholdMinutes);

        // Ищем дубликаты среди ACTIVE и FILLED
        List<Ad> duplicates = adRepository.findDuplicateAds(
                request.getType(),
                request.getSubType(),
                request.getCityId(),
                List.of(Ad.AdStatus.ACTIVE, Ad.AdStatus.FILLED),
                startTimeMinus,
                startTimePlus);

        return duplicates.stream()
                .map(ad -> {
                    // Для каждого дубликата получаем название ЛДС (первого в списке)
                    String rinkName = ad.getAdRinks().stream()
                            .findFirst()
                            .map(adRink -> adRink.getRink().getName())
                            .orElse(null);

                    String filledProgress = "";
                    if (ad.getType() == 1 && ad.getSubType() == 1) {
                        filledProgress = ad.getAcceptedGoaliesCount() + "/" + ad.getGoaliesCount() + " вратарей";
                    } else if (ad.getType() == 1 && ad.getSubType() == 2) {
                        filledProgress = "З:" + ad.getAcceptedDefendersCount() + "/" + ad.getDefendersCount() +
                                " Н:" + ad.getAcceptedForwardsCount() + "/" + ad.getForwardsCount();
                    } else {
                        filledProgress = ad.getAcceptedResponsesCount() + "/1";
                    }

                    return DuplicateAdResponse.builder()
                            .id(ad.getId())
                            .startTime(ad.getStartTime())
                            .rinkName(rinkName)
                            .cityName(ad.getCity().getName())
                            .status(ad.getStatus().name())
                            .filledProgress(filledProgress)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Определяет, нужно ли проверять дублирование для данного типа объявления
     */
    private boolean shouldCheckDuplicate(Integer type, Integer subType) {
        // Тип 1.1 (вратарь) и 1.2 (полевой)
        if (type == 1 && (subType == 1 || subType == 2)) {
            return true;
        }
        // Тип 3.2 (предлагаю матч)
        if (type == 3 && subType == 2) {
            return true;
        }
        // Тип 4.1-4.4 (специалисты)
        if (type == 4 && subType >= 1 && subType <= 4) {
            return true;
        }
        return false;
    }

    /**
     * Создание объявления с проверкой на дублирование
     */
    @Transactional
    public AdResponse createAdWithDuplicateCheck(UUID authorId, AdRequest request) {
        // Проверяем на дублирование
        List<DuplicateAdResponse> duplicates = checkDuplicate(request);

        if (!duplicates.isEmpty()) {
            String message = "Объявление с такими параметрами уже существует. " +
                    "Найдено " + duplicates.size() + " похожих объявлений.";
            throw new BadRequestException(message);
        }

        // Если дубликатов нет, создаём объявление
        return createAd(authorId, request);
    }

    private boolean isSingleRinkType(Integer type, Integer subType) {
        return type == 1 || (type == 3 && subType == 2) || type == 4;
    }

    private boolean requiresEndTime(Integer type, Integer subType) {
        return type == 2 || (type == 3 && subType == 1);
    }

    private boolean requiresLevel(Integer type, Integer subType) {
        return type != 4;
    }



    @Value
    @Builder
    public static class AdStatistics {
        long totalActive;
        long totalModeration;
        long totalFilled;
        long totalArchived;
    }
}