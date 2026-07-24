package info.prorabka.varamy.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class AdFilterRequest {

    private Long cityId;           // null или 0 – все города
    private Integer type;          // 1,2,3,4
    private Integer subType;       // зависит от типа
    private String role;           // DEFENDER или FORWARD (для type=1, subType=2)
    private List<String> level;    // ["A","B"]

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFrom;    // начало диапазона дат

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateTo;      // конец диапазона дат

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime timeFrom;    // начало диапазона времени

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime timeTo;      // конец диапазона времени

    private List<Long> rinkIds;    // список ID катков
}
