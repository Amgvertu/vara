package info.prorabka.varamy.specification;

import info.prorabka.varamy.entity.UserStat;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class UserStatSpecifications {

    public static Specification<UserStat> hasCityIds(List<Long> cityIds) {
        return (root, query, cb) -> {
            if (cityIds == null || cityIds.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("cityId").in(cityIds);
        };
    }

    public static Specification<UserStat> registeredAtBetween(LocalDateTime dateFrom, LocalDateTime dateTo) {
        return (root, query, cb) -> {
            if (dateFrom == null && dateTo == null) {
                return cb.conjunction();
            }
            if (dateFrom != null && dateTo != null) {
                return cb.between(root.get("registeredAt"), dateFrom, dateTo);
            } else if (dateFrom != null) {
                return cb.greaterThanOrEqualTo(root.get("registeredAt"), dateFrom);
            } else {
                return cb.lessThanOrEqualTo(root.get("registeredAt"), dateTo);
            }
        };
    }

    public static Specification<UserStat> hasPositions(List<String> positions) {
        return (root, query, cb) -> {
            if (positions == null || positions.isEmpty()) {
                return cb.conjunction();
            }
            return cb.lower(root.get("position")).in(positions.stream().map(String::toLowerCase).toList());
        };
    }
}
