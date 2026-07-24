package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.response.UserResponse;
import info.prorabka.varamy.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {ProfileMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserResponse toResponse(User user);
}