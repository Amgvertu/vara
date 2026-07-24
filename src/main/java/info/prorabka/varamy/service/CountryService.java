package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.response.CountryResponse;
import info.prorabka.varamy.dto.response.CountrySimpleResponse;
import info.prorabka.varamy.entity.Country;
import info.prorabka.varamy.mapper.CountryMapper;
import info.prorabka.varamy.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;
    private final CountryMapper countryMapper;

    public List<CountryResponse> getAllCountries() {
        return countryRepository.findAll().stream()
                .map(countryMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CountrySimpleResponse> getSimpleCountries() {
        return countryRepository.findAll().stream()
                .map(country -> new CountrySimpleResponse(
                        country.getId(),
                        country.getName(),
                        country.getCode()))
                .collect(Collectors.toList());
    }
}
