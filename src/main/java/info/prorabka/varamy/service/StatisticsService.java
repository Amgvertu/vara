package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.response.AdStatsResponse;
import info.prorabka.varamy.dto.response.UserStatsResponse;
import info.prorabka.varamy.entity.*;
import info.prorabka.varamy.repository.*;
import info.prorabka.varamy.specification.AdStatSpecifications;
import info.prorabka.varamy.specification.AcceptedStatSpecifications;
import info.prorabka.varamy.specification.ResponseStatSpecifications;
import info.prorabka.varamy.specification.UserStatSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserRepository userRepository;
    private final AdRepository adRepository;
    private final ResponseRepository responseRepository;

    private final UserStatRepository userStatRepository;
    private final AdStatRepository adStatRepository;
    private final ResponseStatRepository responseStatRepository;
    private final AcceptedStatRepository acceptedStatRepository;

    // ========== ТЕКУЩИЕ ДАННЫЕ (из основных таблиц) ==========

    /**
     * Количество пользователей в системе СЕЙЧАС (из таблицы users)
     */
    public long getCurrentUsersCount(List<Long> cityIds, LocalDateTime dateFrom, LocalDateTime dateTo, List<String> positions) {
        // TODO: реализовать фильтрацию по cityIds, датам и позициям через Specification для User
        // Пока возвращаем общее количество
        return userRepository.count();
    }

    /**
     * Количество объявлений в системе СЕЙЧАС (из таблицы ads)
     */
    public long getCurrentAdsCount(List<Long> cityIds, LocalDateTime dateFrom, LocalDateTime dateTo, List<String> statuses) {
        // TODO: реализовать фильтрацию через Specification для Ad
        // Пока возвращаем общее количество
        return adRepository.count();
    }

    /**
     * Количество откликов в системе СЕЙЧАС (из таблицы responses)
     */
    public long getCurrentResponsesCount(List<Long> cityIds, LocalDateTime dateFrom, LocalDateTime dateTo) {
        // TODO: реализовать фильтрацию через Specification для Response
        // Пока возвращаем общее количество
        return responseRepository.count();
    }

    /**
     * Количество ПРИНЯТЫХ откликов в системе СЕЙЧАС (из таблицы responses)
     */
    public long getCurrentAcceptedResponsesCount(List<Long> cityIds, LocalDateTime dateFrom, LocalDateTime dateTo) {
        // TODO: реализовать фильтрацию через Specification для Response со статусом APPROVED
        // Пока возвращаем общее количество принятых
        return responseRepository.findAll().stream()
                .filter(r -> r.getStatus() == Response.ResponseStatus.APPROVED)
                .count();
    }

    // ========== КУМУЛЯТИВНЫЕ ДАННЫЕ (из таблиц статистики) ==========

    /**
     * Кумулятивное количество пользователей (из user_stats)
     */
    public long getCumulativeUsersCount(List<Long> cityIds, LocalDateTime dateFrom, LocalDateTime dateTo, List<String> positions) {
        Specification<UserStat> spec = Specification
                .where(UserStatSpecifications.hasCityIds(cityIds))
                .and(UserStatSpecifications.registeredAtBetween(dateFrom, dateTo))
                .and(UserStatSpecifications.hasPositions(positions));
        return userStatRepository.count(spec);
    }

    /**
     * Кумулятивное количество объявлений (из ad_stats)
     */
    public long getCumulativeAdsCount(List<Long> cityIds, LocalDateTime dateFrom, LocalDateTime dateTo) {
        Specification<AdStat> spec = Specification
                .where(AdStatSpecifications.hasCityIds(cityIds))
                .and(AdStatSpecifications.createdAtBetween(dateFrom, dateTo));
        return adStatRepository.count(spec);
    }

    /**
     * Кумулятивное количество откликов (из response_stats)
     */
    public long getCumulativeResponsesCount(List<Long> cityIds, LocalDateTime dateFrom, LocalDateTime dateTo) {
        Specification<ResponseStat> spec = Specification
                .where(ResponseStatSpecifications.hasCityIds(cityIds))
                .and(ResponseStatSpecifications.createdAtBetween(dateFrom, dateTo));
        return responseStatRepository.count(spec);
    }

    /**
     * Кумулятивное количество принятых откликов (из accepted_stats)
     */
    public long getCumulativeAcceptedCount(List<Long> cityIds, LocalDateTime dateFrom, LocalDateTime dateTo) {
        Specification<AcceptedStat> spec = Specification
                .where(AcceptedStatSpecifications.hasCityIds(cityIds))
                .and(AcceptedStatSpecifications.acceptedAtBetween(dateFrom, dateTo));
        return acceptedStatRepository.count(spec);
    }

    // ========== КОМБИНИРОВАННЫЕ ОТВЕТЫ ДЛЯ API ==========

    /**
     * Полная статистика по пользователям (текущая + кумулятивная)
     */
    public UserStatsResponse getUserStats(List<Long> cityIds, LocalDateTime dateFrom, LocalDateTime dateTo, List<String> positions) {
        long current = getCurrentUsersCount(cityIds, dateFrom, dateTo, positions);
        long cumulative = getCumulativeUsersCount(cityIds, dateFrom, dateTo, positions);
        return new UserStatsResponse(current, cumulative);
    }

    /**
     * Полная статистика по объявлениям (текущая + кумулятивная)
     */
    public AdStatsResponse getAdStats(List<Long> cityIds, LocalDateTime dateFrom, LocalDateTime dateTo, List<String> statuses) {
        // Текущие данные (из основных таблиц)
        long currentAds = getCurrentAdsCount(cityIds, dateFrom, dateTo, statuses);
        long currentResponses = getCurrentResponsesCount(cityIds, dateFrom, dateTo);
        long currentAccepted = getCurrentAcceptedResponsesCount(cityIds, dateFrom, dateTo);

        // Кумулятивные данные (из таблиц статистики)
        long cumulativeAds = getCumulativeAdsCount(cityIds, dateFrom, dateTo);
        long cumulativeResponses = getCumulativeResponsesCount(cityIds, dateFrom, dateTo);
        long cumulativeAccepted = getCumulativeAcceptedCount(cityIds, dateFrom, dateTo);

        return new AdStatsResponse(
                currentAds, currentResponses, currentAccepted,
                cumulativeAds, cumulativeResponses, cumulativeAccepted
        );
    }
}