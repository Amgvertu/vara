package info.prorabka.varamy.event;

import info.prorabka.varamy.entity.Response;
import org.springframework.context.ApplicationEvent;

public class ResponseAcceptedEvent extends ApplicationEvent {
    private final Response response;

    public ResponseAcceptedEvent(Response response) {
        super(response);
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }
}