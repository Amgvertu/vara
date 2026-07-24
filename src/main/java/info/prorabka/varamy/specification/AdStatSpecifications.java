package info.prorabka.varamy.specification;

import info.prorabka.varamy.entity.AdStat;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class AdStatSpecifications {

    public static Specification<AdStat> hasCityIds(List<Long> cityIds) {
        return (root, query, cb) -> {
            if (cityIds == null || cityIds.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("cityId").in(cityIds);
        };
    }

    public static Specification<AdStat> createdAtBetween(LocalDateTime dateFrom, LocalDateTime dateTo) {
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
