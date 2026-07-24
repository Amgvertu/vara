package info.prorabka.varamy.specification;

import info.prorabka.varamy.entity.Profile;
import info.prorabka.varamy.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public class UserSpecifications {

    public static Specification<User> searchBy(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.toLowerCase() + "%";
            Join<User, Profile> profile = root.join("profile", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("phone")), pattern),
                    cb.like(cb.lower(profile.get("firstName")), pattern),
                    cb.like(cb.lower(profile.get("lastName")), pattern),
                    cb.like(cb.lower(profile.get("email")), pattern),
                    cb.like(cb.lower(cb.function("CAST", String.class, root.get("id"))), pattern)
            );
        };
    }

    public static Specification<User> hasRoles(List<String> roles) {
        return (root, query, cb) -> {
            if (roles == null || roles.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("role").in(roles);
        };
    }

    public static Specification<User> hasStatuses(List<String> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<User> hasCityIds(List<Long> cityIds) {
        return (root, query, cb) -> {
            if (cityIds == null || cityIds.isEmpty()) {
                return cb.conjunction();
            }
            Join<User, Profile> profile = root.join("profile", JoinType.LEFT);
            return profile.get("homeCity").get("id").in(cityIds);
        };
    }

    public static Specification<User> hasTeams(List<String> teams) {
        return (root, query, cb) -> {
            if (teams == null || teams.isEmpty()) {
                return cb.conjunction();
            }
            Join<User, Profile> profile = root.join("profile", JoinType.LEFT);
            return cb.lower(profile.get("team")).in(teams.stream().map(String::toLowerCase).toList());
        };
    }
}