package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.response.CountryResponse;
import info.prorabka.varamy.entity.Country;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CountryMapper {

    CountryResponse toResponse(Country country);
}
