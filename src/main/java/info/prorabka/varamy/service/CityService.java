package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.response.CityResponse;
import info.prorabka.varamy.dto.response.CitySimpleResponse;
import info.prorabka.varamy.entity.City;
import info.prorabka.varamy.exception.ResourceNotFoundException;
import info.prorabka.varamy.mapper.CityMapper;
import info.prorabka.varamy.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final CityMapper cityMapper;

    public List<CityResponse> getCitiesByRegion(Long regionId) {
        return cityRepository.findByRegionId(regionId).stream()
                .map(cityMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CitySimpleResponse> getSimpleCitiesByRegion(Long regionId) {
        return cityRepository.findByRegionId(regionId).stream()
                .map(city -> new CitySimpleResponse(city.getId(), city.getName()))
                .collect(Collectors.toList());
    }

    public List<CityResponse> getCitiesByCountry(Long countryId) {
        return cityRepository.findByCountryId(countryId).stream()
                .map(cityMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CitySimpleResponse> getSimpleCitiesByCountry(Long countryId) {
        return cityRepository.findByCountryId(countryId).stream()
                .map(city -> new CitySimpleResponse(city.getId(), city.getName()))
                .collect(Collectors.toList());
    }

    public List<CitySimpleResponse> getAllCitiesSimple() {
        return cityRepository.findAll().stream()
                .map(c -> new CitySimpleResponse(c.getId(), c.getName()))
                .collect(Collectors.toList());
    }
}
