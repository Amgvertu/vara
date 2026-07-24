package info.prorabka.varamy.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String team;
    private String position;
    private String level;
    private Integer number;
    private LocalDate birthDate;
    private Long homeCityId;
}