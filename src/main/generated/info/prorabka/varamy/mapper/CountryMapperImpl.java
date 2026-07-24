package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.response.CountryResponse;
import info.prorabka.varamy.entity.Country;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-27T23:55:30+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class CountryMapperImpl implements CountryMapper {

    @Override
    public CountryResponse toResponse(Country country) {
        if ( country == null ) {
            return null;
        }

        CountryResponse countryResponse = new CountryResponse();

        countryResponse.setId( country.getId() );
        countryResponse.setName( country.getName() );
        countryResponse.setCode( country.getCode() );
        countryResponse.setPhoneCode( country.getPhoneCode() );

        return countryResponse;
    }
}
