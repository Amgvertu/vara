package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.response.RegionResponse;
import info.prorabka.varamy.entity.Region;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-27T23:55:30+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class RegionMapperImpl implements RegionMapper {

    @Autowired
    private CountryMapper countryMapper;

    @Override
    public RegionResponse toResponse(Region region) {
        if ( region == null ) {
            return null;
        }

        RegionResponse regionResponse = new RegionResponse();

        regionResponse.setId( region.getId() );
        regionResponse.setName( region.getName() );
        regionResponse.setCountry( countryMapper.toResponse( region.getCountry() ) );
        regionResponse.setAutoCode( region.getAutoCode() );

        return regionResponse;
    }
}
