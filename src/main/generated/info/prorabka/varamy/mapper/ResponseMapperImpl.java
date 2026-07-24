package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.response.ResponseResponse;
import info.prorabka.varamy.entity.Ad;
import info.prorabka.varamy.entity.Response;
import info.prorabka.varamy.entity.User;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-27T23:55:30+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class ResponseMapperImpl implements ResponseMapper {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ResponseResponse toResponse(Response response) {
        if ( response == null ) {
            return null;
        }

        ResponseResponse responseResponse = new ResponseResponse();

        responseResponse.setAdId( responseAdId( response ) );
        responseResponse.setUser( userMapper.toResponse( response.getUser() ) );
        responseResponse.setUserId( responseUserId( response ) );
        responseResponse.setResponseRole( response.getResponseRole() );
        responseResponse.setId( response.getId() );
        responseResponse.setStatus( response.getStatus() );
        responseResponse.setMessage( response.getMessage() );
        responseResponse.setCreatedAt( response.getCreatedAt() );

        return responseResponse;
    }

    private UUID responseAdId(Response response) {
        if ( response == null ) {
            return null;
        }
        Ad ad = response.getAd();
        if ( ad == null ) {
            return null;
        }
        UUID id = ad.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private UUID responseUserId(Response response) {
        if ( response == null ) {
            return null;
        }
        User user = response.getUser();
        if ( user == null ) {
            return null;
        }
        UUID id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
