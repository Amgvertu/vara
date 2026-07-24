package info.prorabka.varamy.entity;

import lombok.Data;
import java.util.List;

@Data
public class AdDetails {
    // type=1 (Ищу игрока)
    private String role; // "вратарь" или "полевой"
    private Integer countPlayers;
    private Integer defenders;
    private Integer forwards;
    private String delivery; // "yes"/"no"
    private String payment;

    // type=2,3 (Ищу игру)
    private String endTime;

    // type=4 (Ищу команду для матча)
    private String team;

    // type=5 (Ищу специалиста)
    private List<String> specialists;
}
