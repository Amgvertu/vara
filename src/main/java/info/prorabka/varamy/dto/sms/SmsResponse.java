package info.prorabka.varamy.dto.sms;

import lombok.Data;

@Data
public class SmsResponse {
    private String requestId;
    private boolean success;
    private String errorMessage;
}
