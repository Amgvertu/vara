package info.prorabka.varamy.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsCommand {
    private String requestId;
    private String phone;
    private String code;
    private String purpose; // REGISTRATION, PASSWORD_RESET, PHONE_CHANGE
}
