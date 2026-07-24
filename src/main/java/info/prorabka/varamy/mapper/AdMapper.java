package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.request.AdRequest;
import info.prorabka.varamy.dto.response.AdResponse;
import info.prorabka.varamy.entity.Ad;
import info.prorabka.varamy.entity.AdRink;
import info.prorabka.varamy.entity.City;
import info.prorabka.varamy.entity.Rink;
import info.prorabka.varamy.repository.RinkRepository;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {UserMapper.class, CityMapper.class, ResponseMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdMapper {

    @Mapping(source = "author", target = "author")
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "city", target = "city")
    @Mapping(source = "levels", target = "level")
    @Mapping(source = "responses", target = "responses")
    @Mapping(source = "subType", target = "subType")
    @Mapping(source = "endTime", target = "endTime")
    @Mapping(source = "showTeam", target = "showTeam")
    @Mapping(source = "goaliesCount", target = "goaliesCount")
    @Mapping(source = "defendersCount", target = "defendersCount")
    @Mapping(source = "forwardsCount", target = "forwardsCount")
    @Mapping(source = "acceptedGoaliesCount", target = "acceptedGoaliesCount")
    @Mapping(source = "acceptedDefendersCount", target = "acceptedDefendersCount")
    @Mapping(source = "acceptedForwardsCount", target = "acceptedForwardsCount")
    @Mapping(target = "rinkIds", expression = "java(mapAdRinksToRinkIds(ad))")
    AdResponse toResponse(Ad ad);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "responses", ignore = true)
    @Mapping(target = "levels", source = "level")
    @Mapping(target = "city", source = "cityId", qualifiedByName = "cityIdToCity")
    @Mapping(target = "subType", source = "subType")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "showTeam", source = "showTeam")
    @Mapping(target = "goaliesCount", source = "goaliesCount")
    @Mapping(target = "defendersCount", source = "defendersCount")
    @Mapping(target = "forwardsCount", source = "forwardsCount")
    @Mapping(target = "acceptedGoaliesCount", ignore = true)
    @Mapping(target = "acceptedDefendersCount", ignore = true)
    @Mapping(target = "acceptedForwardsCount", ignore = true)
    @Mapping(target = "adRinks", ignore = true)   // заполним в сервисе
        // ⚠️ НЕТ строки @Mapping(target = "rinkIds", ...) – её не должно быть!
    Ad toEntity(AdRequest request, @Context City city);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "responses", ignore = true)
    @Mapping(target = "levels", source = "level")
    @Mapping(target = "city", source = "cityId", qualifiedByName = "cityIdToCity")
    @Mapping(target = "subType", source = "subType")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "showTeam", source = "showTeam")
    @Mapping(target = "goaliesCount", source = "goaliesCount")
    @Mapping(target = "defendersCount", source = "defendersCount")
    @Mapping(target = "forwardsCount", source = "forwardsCount")
    @Mapping(target = "acceptedGoaliesCount", ignore = true)
    @Mapping(target = "acceptedDefendersCount", ignore = true)
    @Mapping(target = "acceptedForwardsCount", ignore = true)
    @Mapping(target = "adRinks", ignore = true)
        // ⚠️ И здесь нет @Mapping(target = "rinkIds", ...)
    void updateAd(@MappingTarget Ad ad, AdRequest request, @Context City city);

    // Кастомный метод для конвертации adRinks -> List<Long>
    default List<Long> mapAdRinksToRinkIds(Ad ad) {
        if (ad == null || ad.getAdRinks() == null) {
            return List.of();
        }
        return ad.getAdRinks().stream()
                .map(adRink -> adRink.getRink().getId())
                .collect(Collectors.toList());
    }

    @Named("cityIdToCity")
    default City cityIdToCity(Long cityId, @Context City city) {
        return city;
    }

}