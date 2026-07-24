package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.response.RinkResponse;
import info.prorabka.varamy.entity.Rink;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {CountryMapper.class, RegionMapper.class, CityMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RinkMapper {

    RinkResponse toResponse(Rink rink);
}
