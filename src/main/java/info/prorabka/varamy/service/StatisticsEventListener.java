package info.prorabka.varamy.service;

import info.prorabka.varamy.entity.*;
import info.prorabka.varamy.event.AdCreatedEvent;
import info.prorabka.varamy.event.ResponseAcceptedEvent;
import info.prorabka.varamy.event.ResponseCreatedEvent;
import info.prorabka.varamy.event.UserRegisteredEvent;
import info.prorabka.varamy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class StatisticsEventListener {

    private final UserStatRepository userStatRepository;
    private final AdStatRepository adStatRepository;
    private final ResponseStatRepository responseStatRepository;
    private final AcceptedStatRepository acceptedStatRepository;

    // Слушатель регистрации пользователя
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        User user = event.getUser();
        UserStat stat = new UserStat();
        stat.setUserId(user.getId());
        stat.setRegisteredAt(user.getRegisteredAt());
        if (user.getProfile() != null) {
            stat.setCityId(user.getProfile().getHomeCity() != null ? user.getProfile().getHomeCity().getId() : null);
            stat.setPosition(user.getProfile().getPosition());
        }
        userStatRepository.save(stat);
    }

    // Слушатель создания объявления
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAdCreated(AdCreatedEvent event) {
        Ad ad = event.getAd();
        AdStat stat = new AdStat();
        stat.setAdId(ad.getId());
        stat.setCreatedAt(ad.getCreatedAt());
        stat.setCityId(ad.getCity() != null ? ad.getCity().getId() : null);
        adStatRepository.save(stat);
    }

    // Слушатель создания отклика
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleResponseCreated(ResponseCreatedEvent event) {
        Response response = event.getResponse();
        ResponseStat stat = new ResponseStat();
        stat.setResponseId(response.getId());
        stat.setAdId(response.getAd().getId());
        stat.setCreatedAt(response.getCreatedAt());
        responseStatRepository.save(stat);
    }

    // Слушатель принятия отклика
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleResponseAccepted(ResponseAcceptedEvent event) {
        Response response = event.getResponse();
        AcceptedStat stat = new AcceptedStat();
        stat.setResponseId(response.getId());
        stat.setAdId(response.getAd().getId());
        stat.setAcceptedAt(LocalDateTime.now());
        acceptedStatRepository.save(stat);
    }
}