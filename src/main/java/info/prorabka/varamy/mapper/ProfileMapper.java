package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.request.ProfileRequest;
import info.prorabka.varamy.dto.response.ProfileResponse;
import info.prorabka.varamy.entity.Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {CountryMapper.class, RegionMapper.class, CityMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProfileMapper {

    @Mapping(source = "homeCountry", target = "homeCountry")
    @Mapping(source = "homeRegion", target = "homeRegion")
    @Mapping(source = "homeCity", target = "homeCity")
    ProfileResponse toResponse(Profile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "homeCountry", ignore = true)
    @Mapping(target = "homeRegion", ignore = true)
    @Mapping(target = "homeCity", ignore = true)
    void updateProfile(@MappingTarget Profile profile, ProfileRequest request);
}
