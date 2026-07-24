package info.prorabka.varamy.mapper;

import info.prorabka.varamy.dto.response.ResponseResponse;
import info.prorabka.varamy.entity.Response;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {UserMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResponseMapper {

    @Mapping(source = "ad.id", target = "adId")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "responseRole", target = "responseRole")
    ResponseResponse toResponse(Response response);
}