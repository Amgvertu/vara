package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.sms.SmsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class SmsGatewayController {

    @MessageMapping("/sms-response")
    public void handleSmsResponse(SmsResponse response) {
        log.info("Received SMS gateway response: requestId={}, success={}, error={}",
                response.getRequestId(), response.isSuccess(), response.getErrorMessage());
        // Здесь можно обновить статус отправки в БД, если сохраняли requestId
    }
}