package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.request.ProfileRequest;
import info.prorabka.varamy.dto.response.ProfileResponse;
import info.prorabka.varamy.entity.Profile;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-27T23:55:30+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class ProfileMapperImpl implements ProfileMapper {

    @Autowired
    private CountryMapper countryMapper;
    @Autowired
    private RegionMapper regionMapper;
    @Autowired
    private CityMapper cityMapper;

    @Override
    public ProfileResponse toResponse(Profile profile) {
        if ( profile == null ) {
            return null;
        }

        ProfileResponse profileResponse = new ProfileResponse();

        profileResponse.setHomeCountry( countryMapper.toResponse( profile.getHomeCountry() ) );
        profileResponse.setHomeRegion( regionMapper.toResponse( profile.getHomeRegion() ) );
        profileResponse.setHomeCity( cityMapper.toResponse( profile.getHomeCity() ) );
        profileResponse.setId( profile.getId() );
        profileResponse.setFirstName( profile.getFirstName() );
        profileResponse.setLastName( profile.getLastName() );
        profileResponse.setBirthDate( profile.getBirthDate() );
        profileResponse.setPosition( profile.getPosition() );
        profileResponse.setLevel( profile.getLevel() );
        profileResponse.setNumber( profile.getNumber() );
        profileResponse.setTeam( profile.getTeam() );
        profileResponse.setEmail( profile.getEmail() );
        profileResponse.setAvatarUrl( profile.getAvatarUrl() );

        return profileResponse;
    }

    @Override
    public void updateProfile(Profile profile, ProfileRequest request) {
        if ( request == null ) {
            return;
        }

        profile.setFirstName( request.getFirstName() );
        profile.setLastName( request.getLastName() );
        profile.setBirthDate( request.getBirthDate() );
        profile.setPosition( request.getPosition() );
        profile.setLevel( request.getLevel() );
        profile.setNumber( request.getNumber() );
        profile.setTeam( request.getTeam() );
        profile.setEmail( request.getEmail() );
    }
}
