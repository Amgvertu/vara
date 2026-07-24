package info.prorabka.varamy.service;

import info.prorabka.varamy.entity.Agreement;
import info.prorabka.varamy.repository.AgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgreementService {

    private final AgreementRepository agreementRepository;

    @Transactional(readOnly = true)
    public Optional<Agreement> getAgreementByType(String type) {
        return agreementRepository.findByType(type);
    }
}