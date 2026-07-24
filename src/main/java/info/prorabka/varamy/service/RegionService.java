package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.response.RegionResponse;
import info.prorabka.varamy.dto.response.RegionSimpleResponse;
import info.prorabka.varamy.entity.Region;
import info.prorabka.varamy.exception.ResourceNotFoundException;
import info.prorabka.varamy.mapper.RegionMapper;
import info.prorabka.varamy.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;
    private final RegionMapper regionMapper;

    public List<RegionResponse> getRegionsByCountry(Long countryId) {
        return regionRepository.findByCountryId(countryId).stream()
                .map(regionMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<RegionSimpleResponse> getSimpleRegionsByCountry(Long countryId) {
        return regionRepository.findByCountryId(countryId).stream()
                .map(region -> new RegionSimpleResponse(
                        region.getId(),
                        region.getName(),
                        region.getAutoCode()))
                .collect(Collectors.toList());
    }
}