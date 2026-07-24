package info.prorabka.varamy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserSearchResponse {
    private UUID id;
    private String phone;
    private ProfileSimpleResponse profile;
}