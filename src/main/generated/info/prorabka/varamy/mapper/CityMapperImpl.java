package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.response.CityResponse;
import info.prorabka.varamy.entity.City;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-27T23:55:30+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class CityMapperImpl implements CityMapper {

    @Autowired
    private RegionMapper regionMapper;

    @Override
    public CityResponse toResponse(City city) {
        if ( city == null ) {
            return null;
        }

        CityResponse cityResponse = new CityResponse();

        cityResponse.setId( city.getId() );
        cityResponse.setName( city.getName() );
        cityResponse.setRegion( regionMapper.toResponse( city.getRegion() ) );

        return cityResponse;
    }
}
