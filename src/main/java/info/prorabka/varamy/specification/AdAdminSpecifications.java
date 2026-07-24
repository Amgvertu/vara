package info.prorabka.varamy.specification;

import info.prorabka.varamy.entity.Ad;
import info.prorabka.varamy.entity.Profile;
import info.prorabka.varamy.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public class AdAdminSpecifications {

    public static Specification<Ad> hasCityIds(List<Long> cityIds) {
        return (root, query, cb) -> {
            if (cityIds == null || cityIds.isEmpty()) return cb.conjunction();
            return root.get("city").get("id").in(cityIds);
        };
    }

    public static Specification<Ad> hasTypes(List<Integer> types) {
        return (root, query, cb) -> {
            if (types == null || types.isEmpty()) return cb.conjunction();
            return root.get("type").in(types);
        };
    }

    public static Specification<Ad> hasSubTypes(List<Integer> subTypes) {
        return (root, query, cb) -> {
            if (subTypes == null || subTypes.isEmpty()) return cb.conjunction();
            return root.get("subType").in(subTypes);
        };
    }

    public static Specification<Ad> hasStatuses(List<Ad.AdStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) return cb.conjunction();
            return root.get("status").in(statuses);
        };
    }

    public static Specification<Ad> hasLevels(List<String> levels) {
        return (root, query, cb) -> {
            if (levels == null || levels.isEmpty()) return cb.conjunction();
            return root.join("levels", JoinType.LEFT).in(levels);
        };
    }

    public static Specification<Ad> hasAuthorIds(List<UUID> authorIds) {
        return (root, query, cb) -> {
            if (authorIds == null || authorIds.isEmpty()) return cb.conjunction();
            return root.get("author").get("id").in(authorIds);
        };
    }

    public static Specification<Ad> hasRinkIds(List<Long> rinkIds) {
        return (root, query, cb) -> {
            if (rinkIds == null || rinkIds.isEmpty()) return cb.conjunction();
            Join<Object, Object> adRinks = root.join("adRinks", JoinType.LEFT);
            return adRinks.get("rink").get("id").in(rinkIds);
        };
    }

    public static Specification<Ad> searchBy(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.toLowerCase() + "%";
            Join<Ad, User> author = root.join("author", JoinType.LEFT);
            Join<User, Profile> profile = author.join("profile", JoinType.LEFT);
            Join<Ad, ?> rinkJoin = root.join("adRinks", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("city").get("name")), pattern),
                    cb.like(cb.lower(rinkJoin.get("rink").get("name")), pattern),
                    cb.like(cb.lower(author.get("phone")), pattern),
                    cb.like(cb.lower(profile.get("firstName")), pattern),
                    cb.like(cb.lower(profile.get("lastName")), pattern)
            );
        };
    }
}