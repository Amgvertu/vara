package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.response.CityResponse;
import info.prorabka.varamy.entity.City;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {RegionMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CityMapper {

    CityResponse toResponse(City city);
}
