package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.response.UserResponse;
import info.prorabka.varamy.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-27T23:55:30+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Autowired
    private ProfileMapper profileMapper;

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse userResponse = new UserResponse();

        userResponse.setId( user.getId() );
        userResponse.setPhone( user.getPhone() );
        userResponse.setRole( user.getRole() );
        userResponse.setSubrole( user.getSubrole() );
        userResponse.setStatus( user.getStatus() );
        userResponse.setRegisteredAt( user.getRegisteredAt() );
        userResponse.setLastLoginAt( user.getLastLoginAt() );
        userResponse.setProfile( profileMapper.toResponse( user.getProfile() ) );

        return userResponse;
    }
}
