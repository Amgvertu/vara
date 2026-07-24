package info.prorabka.varamy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserStatsResponse {
    private long currentUsers;      // сколько пользователей СЕЙЧАС
    private long cumulativeUsers;   // сколько всего было зарегистрировано (история)
}
