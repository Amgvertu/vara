package info.prorabka.varamy.specification;

import info.prorabka.varamy.entity.ResponseStat;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class ResponseStatSpecifications {

    public static Specification<ResponseStat> hasCityIds(List<Long> cityIds) {
        return (root, query, cb) -> {
            if (cityIds == null || cityIds.isEmpty()) {
                return cb.conjunction();
            }
            // cityId хранится в ResponseStat, но мы должны соединиться с AdStat
            // Используем подзапрос для фильтрации по городам
            var subquery = query.subquery(Long.class);
            var adStatRoot = subquery.from(info.prorabka.varamy.entity.AdStat.class);
            subquery.select(adStatRoot.get("id"));
            subquery.where(
                    cb.equal(adStatRoot.get("adId"), root.get("adId")),
                    adStatRoot.get("cityId").in(cityIds)
            );
            return cb.exists(subquery);
        };
    }

    public static Specification<ResponseStat> createdAtBetween(LocalDateTime dateFrom, LocalDateTime dateTo) {
        return (root, query, cb) -> {
            if (dateFrom == null && dateTo == null) {
                return cb.conjunction();
            }
            if (dateFrom != null && dateTo != null) {
                return cb.between(root.get("createdAt"), dateFrom, dateTo);
            } else if (dateFrom != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom);
            } else {
                return cb.lessThanOrEqualTo(root.get("createdAt"), dateTo);
            }
        };
    }
}