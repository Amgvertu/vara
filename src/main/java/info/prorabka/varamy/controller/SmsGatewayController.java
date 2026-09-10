package info.prorabka.varamy.controller;

import info.prorabka.varamy.dto.sms.SmsResponse;
import info.prorabka.varamy.service.SmsGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class SmsGatewayController {

    private final SmsGatewayService smsGatewayService;

    @MessageMapping("/sms-response")
    public void handleSmsResponse(SmsResponse response) {
        log.info("Received SMS gateway response: requestId={}, success={}, error={}",
                response.getRequestId(), response.isSuccess(), response.getErrorMessage());
        smsGatewayService.handleSmsResponse(response);
    }
}