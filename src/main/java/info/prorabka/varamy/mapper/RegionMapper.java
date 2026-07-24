package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.response.RegionResponse;
import info.prorabka.varamy.entity.Region;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {CountryMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RegionMapper {

    RegionResponse toResponse(Region region);
}
