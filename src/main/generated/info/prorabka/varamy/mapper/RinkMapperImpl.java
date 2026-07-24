package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.response.RinkResponse;
import info.prorabka.varamy.entity.Rink;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-27T23:55:30+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class RinkMapperImpl implements RinkMapper {

    @Autowired
    private CountryMapper countryMapper;
    @Autowired
    private RegionMapper regionMapper;
    @Autowired
    private CityMapper cityMapper;

    @Override
    public RinkResponse toResponse(Rink rink) {
        if ( rink == null ) {
            return null;
        }

        RinkResponse rinkResponse = new RinkResponse();

        rinkResponse.setId( rink.getId() );
        rinkResponse.setName( rink.getName() );
        rinkResponse.setCountry( countryMapper.toResponse( rink.getCountry() ) );
        rinkResponse.setRegion( regionMapper.toResponse( rink.getRegion() ) );
        rinkResponse.setCity( cityMapper.toResponse( rink.getCity() ) );
        rinkResponse.setAddress( rink.getAddress() );
        rinkResponse.setPhone( rink.getPhone() );
        rinkResponse.setRating( rink.getRating() );
        rinkResponse.setLatitude( rink.getLatitude() );
        rinkResponse.setLongitude( rink.getLongitude() );
        rinkResponse.setFeatures( stringListToStringArray( rink.getFeatures() ) );

        return rinkResponse;
    }

    protected String[] stringListToStringArray(List<String> list) {
        if ( list == null ) {
            return null;
        }

        String[] stringTmp = new String[list.size()];
        int i = 0;
        for ( String string : list ) {
            stringTmp[i] = string;
            i++;
        }

        return stringTmp;
    }
}
