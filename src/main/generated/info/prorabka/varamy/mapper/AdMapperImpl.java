package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.request.AdRequest;
import info.prorabka.varamy.dto.response.AdResponse;
import info.prorabka.varamy.dto.response.ResponseResponse;
import info.prorabka.varamy.entity.Ad;
import info.prorabka.varamy.entity.City;
import info.prorabka.varamy.entity.Response;
import info.prorabka.varamy.entity.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-17T00:39:23+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class AdMapperImpl implements AdMapper {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CityMapper cityMapper;
    @Autowired
    private ResponseMapper responseMapper;

    @Override
    public AdResponse toResponse(Ad ad) {
        if ( ad == null ) {
            return null;
        }

        AdResponse adResponse = new AdResponse();

        adResponse.setAuthor( userMapper.toResponse( ad.getAuthor() ) );
        adResponse.setAuthorId( adAuthorId( ad ) );
        adResponse.setCity( cityMapper.toResponse( ad.getCity() ) );
        List<String> list = ad.getLevels();
        if ( list != null ) {
            adResponse.setLevel( new ArrayList<String>( list ) );
        }
        adResponse.setResponses( responseListToResponseResponseList( ad.getResponses() ) );
        adResponse.setSubType( ad.getSubType() );
        adResponse.setEndTime( ad.getEndTime() );
        adResponse.setShowTeam( ad.getShowTeam() );
        adResponse.setGoaliesCount( ad.getGoaliesCount() );
        adResponse.setDefendersCount( ad.getDefendersCount() );
        adResponse.setForwardsCount( ad.getForwardsCount() );
        adResponse.setAcceptedGoaliesCount( ad.getAcceptedGoaliesCount() );
        adResponse.setAcceptedDefendersCount( ad.getAcceptedDefendersCount() );
        adResponse.setAcceptedForwardsCount( ad.getAcceptedForwardsCount() );
        adResponse.setId( ad.getId() );
        adResponse.setType( ad.getType() );
        adResponse.setStatus( ad.getStatus() );
        adResponse.setCreatedAt( ad.getCreatedAt() );
        adResponse.setStartTime( ad.getStartTime() );
        adResponse.setTeam( ad.getTeam() );
        adResponse.setContactName( ad.getContactName() );
        adResponse.setContactPhone( ad.getContactPhone() );
        adResponse.setDetails( ad.getDetails() );

        adResponse.setRinkIds( mapAdRinksToRinkIds(ad) );

        return adResponse;
    }

    @Override
    public Ad toEntity(AdRequest request, City city) {
        if ( request == null ) {
            return null;
        }

        Ad ad = new Ad();

        List<String> list = request.getLevel();
        if ( list != null ) {
            ad.setLevels( new ArrayList<String>( list ) );
        }
        ad.setCity( cityIdToCity( request.getCityId(), city ) );
        ad.setSubType( request.getSubType() );
        ad.setEndTime( request.getEndTime() );
        ad.setShowTeam( request.getShowTeam() );
        ad.setGoaliesCount( request.getGoaliesCount() );
        ad.setDefendersCount( request.getDefendersCount() );
        ad.setForwardsCount( request.getForwardsCount() );
        ad.setType( request.getType() );
        ad.setStartTime( request.getStartTime() );
        ad.setTeam( request.getTeam() );
        ad.setContactName( request.getContactName() );
        ad.setContactPhone( request.getContactPhone() );
        ad.setDetails( request.getDetails() );

        return ad;
    }

    @Override
    public void updateAd(Ad ad, AdRequest request, City city) {
        if ( request == null ) {
            return;
        }

        if ( ad.getLevels() != null ) {
            List<String> list = request.getLevel();
            if ( list != null ) {
                ad.getLevels().clear();
                ad.getLevels().addAll( list );
            }
        }
        else {
            List<String> list = request.getLevel();
            if ( list != null ) {
                ad.setLevels( new ArrayList<String>( list ) );
            }
        }
        if ( request.getCityId() != null ) {
            ad.setCity( cityIdToCity( request.getCityId(), city ) );
        }
        if ( request.getSubType() != null ) {
            ad.setSubType( request.getSubType() );
        }
        if ( request.getEndTime() != null ) {
            ad.setEndTime( request.getEndTime() );
        }
        if ( request.getShowTeam() != null ) {
            ad.setShowTeam( request.getShowTeam() );
        }
        if ( request.getGoaliesCount() != null ) {
            ad.setGoaliesCount( request.getGoaliesCount() );
        }
        if ( request.getDefendersCount() != null ) {
            ad.setDefendersCount( request.getDefendersCount() );
        }
        if ( request.getForwardsCount() != null ) {
            ad.setForwardsCount( request.getForwardsCount() );
        }
        if ( request.getType() != null ) {
            ad.setType( request.getType() );
        }
        if ( request.getStatus() != null ) {
            ad.setStatus( request.getStatus() );
        }
        if ( request.getStartTime() != null ) {
            ad.setStartTime( request.getStartTime() );
        }
        if ( request.getTeam() != null ) {
            ad.setTeam( request.getTeam() );
        }
        if ( request.getContactName() != null ) {
            ad.setContactName( request.getContactName() );
        }
        if ( request.getContactPhone() != null ) {
            ad.setContactPhone( request.getContactPhone() );
        }
        if ( request.getDetails() != null ) {
            ad.setDetails( request.getDetails() );
        }
    }

    private UUID adAuthorId(Ad ad) {
        if ( ad == null ) {
            return null;
        }
        User author = ad.getAuthor();
        if ( author == null ) {
            return null;
        }
        UUID id = author.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected List<ResponseResponse> responseListToResponseResponseList(List<Response> list) {
        if ( list == null ) {
            return null;
        }

        List<ResponseResponse> list1 = new ArrayList<ResponseResponse>( list.size() );
        for ( Response response : list ) {
            list1.add( responseMapper.toResponse( response ) );
        }

        return list1;
    }
}
