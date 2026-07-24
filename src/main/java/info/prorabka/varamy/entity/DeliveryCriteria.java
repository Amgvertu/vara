package info.prorabka.varamy.entity;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DeliveryCriteria {
    private boolean allUsers;      // отправлять всем пользователям (USER)
    private boolean admins;        // включать администраторов
    private boolean moderators;    // включать модераторов
    private boolean allCities;     // true – фильтр по городам отключён
    private List<Long> cityIds;    // если allCities=false, применяется фильтр
    private boolean allTeams;      // true – фильтр по командам отключён
    private List<String> teamNames;// если allTeams=false, применяется фильтр
    private List<UUID> userIds;    // индивидуальная рассылка (игнорирует всё остальное)
}