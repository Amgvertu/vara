package info.prorabka.varamy.specification;

import info.prorabka.varamy.entity.Ad;
import info.prorabka.varamy.entity.AdRink;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AdSpecifications {

    // Только активные и заполненные объявления
    public static Specification<Ad> hasStatuses() {
        return (root, query, cb) ->
                root.get("status").in(Ad.AdStatus.ACTIVE, Ad.AdStatus.FILLED);
    }

    public static Specification<Ad> hasCityId(Long cityId) {
        return (root, query, cb) -> {
            if (cityId == null || cityId == 0) return cb.conjunction();
            return cb.equal(root.get("city").get("id"), cityId);
        };
    }

    public static Specification<Ad> hasType(Integer type) {
        return (root, query, cb) ->
                type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<Ad> hasSubType(Integer subType) {
        return (root, query, cb) ->
                subType == null ? cb.conjunction() : cb.equal(root.get("subType"), subType);
    }

    public static Specification<Ad> hasRole(String role) {
        return (root, query, cb) -> {
            if (role == null) return cb.conjunction();
            if ("DEFENDER".equalsIgnoreCase(role)) {
                return cb.greaterThan(root.get("defendersCount"), 0);
            } else if ("FORWARD".equalsIgnoreCase(role)) {
                return cb.greaterThan(root.get("forwardsCount"), 0);
            }
            return cb.conjunction();
        };
    }

    public static Specification<Ad> hasLevels(List<String> levels) {
        return (root, query, cb) -> {
            if (levels == null || levels.isEmpty()) return cb.conjunction();
            Join<Ad, String> levelsJoin = root.join("levels", JoinType.LEFT);
            return levelsJoin.in(levels);
        };
    }

    public static Specification<Ad> dateBetween(LocalDate dateFrom, LocalDate dateTo) {
        return (root, query, cb) -> {
            if (dateFrom == null && dateTo == null) return cb.conjunction();
            Path<java.time.LocalDateTime> startTime = root.get("startTime");
            if (dateFrom != null && dateTo != null) {
                return cb.between(
                        startTime,
                        dateFrom.atStartOfDay(),
                        dateTo.atTime(LocalTime.MAX)
                );
            } else if (dateFrom != null) {
                return cb.greaterThanOrEqualTo(startTime, dateFrom.atStartOfDay());
            } else {
                return cb.lessThanOrEqualTo(startTime, dateTo.atTime(LocalTime.MAX));
            }
        };
    }

    public static Specification<Ad> timeBetween(LocalTime timeFrom, LocalTime timeTo) {
        return (root, query, cb) -> {
            if (timeFrom == null && timeTo == null) return cb.conjunction();
            Path<java.time.LocalDateTime> startTime = root.get("startTime");
            // Извлекаем время из LocalDateTime с помощью SQL-функции time()
            Expression<LocalTime> timeExpr = cb.function("time", LocalTime.class, startTime);
            if (timeFrom != null && timeTo != null) {
                return cb.between(timeExpr, timeFrom, timeTo);
            } else if (timeFrom != null) {
                return cb.greaterThanOrEqualTo(timeExpr, timeFrom);
            } else {
                return cb.lessThanOrEqualTo(timeExpr, timeTo);
            }
        };
    }

    public static Specification<Ad> hasRinkIds(List<Long> rinkIds) {
        return (root, query, cb) -> {
            if (rinkIds == null || rinkIds.isEmpty()) {
                return cb.conjunction();
            }
            // Используем exists с подзапросом
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<AdRink> adRinkRoot = subquery.from(AdRink.class);
            subquery.select(adRinkRoot.get("id").get("adId"));
            subquery.where(
                    cb.equal(adRinkRoot.get("id").get("adId"), root.get("id")),
                    adRinkRoot.get("rink").get("id").in(rinkIds)
            );
            return cb.exists(subquery);
        };
    }
}