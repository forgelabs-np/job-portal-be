package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.country.response.CountryResponse;
import com.jobportal.v1.dto.country.response.CountrySyncResponse;
import com.jobportal.v1.dto.country.response.RestCountryResponse;
import com.jobportal.v1.entity.Country;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.CountryRepository;
import com.jobportal.v1.service.CountrySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CountrySyncServiceImpl implements CountrySyncService {

    private final CountryRepository countryRepository;
    private final WebClient webClient = WebClient.create("https://restcountries.com");

    @Override
    @Transactional
    public CountrySyncResponse syncCountriesFromApi() {
        log.info("Starting country sync from REST Countries API");

        try {
            List<RestCountryResponse> apiCountries = webClient.get()
                    .uri("/v3.1/all?fields=name,cca2,currencies,capital,region,subregion,callingCodes")
                    .retrieve()
                    .bodyToFlux(RestCountryResponse.class)
                    .collectList()
                    .block();

            if (apiCountries == null || apiCountries.isEmpty()) {
                throw new RuntimeException("Failed to fetch countries from API");
            }

            int added = 0;
            int updated = 0;

            for (RestCountryResponse apiCountry : apiCountries) {
                String countryCode = apiCountry.getCca2();
                if (countryCode == null || countryCode.isEmpty()) {
                    continue;
                }

                // Get currency info
                String currencyCode = null;
                String currencyName = null;
                String currencySymbol = null;

                if (apiCountry.getCurrencies() != null && !apiCountry.getCurrencies().isEmpty()) {
                    Map.Entry<String, RestCountryResponse.Currency> firstCurrency =
                            apiCountry.getCurrencies().entrySet().iterator().next();
                    currencyCode = firstCurrency.getKey();
                    currencyName = firstCurrency.getValue().getName();
                    currencySymbol = firstCurrency.getValue().getSymbol();
                }

                // Get phone code
                String phoneCode = null;
                if (apiCountry.getCallingCodes() != null && apiCountry.getCallingCodes().length > 0) {
                    phoneCode = "+" + apiCountry.getCallingCodes()[0];
                }

                String countryName = apiCountry.getName() != null ? apiCountry.getName().getCommon() : null;
                String capital = apiCountry.getCapital() != null && apiCountry.getCapital().length > 0
                        ? apiCountry.getCapital()[0] : null;

                Optional<Country> existingCountry = countryRepository.findByCode(countryCode);

                if (existingCountry.isPresent()) {
                    // Update existing country
                    Country country = existingCountry.get();
                    country.setName(countryName != null ? countryName : country.getName());
                    country.setPhoneCode(phoneCode);
                    country.setCurrencyCode(currencyCode != null ? currencyCode : country.getCurrencyCode());
                    country.setCurrencyName(currencyName != null ? currencyName : country.getCurrencyName());
                    country.setCurrencySymbol(currencySymbol);
                    country.setCapital(capital);
                    country.setRegion(apiCountry.getRegion());
                    country.setSubregion(apiCountry.getSubregion());
                    countryRepository.save(country);
                    updated++;
                } else {
                    // Add new country
                    Country country = new Country();
                    country.setCode(countryCode);
                    country.setName(countryName);
                    country.setPhoneCode(phoneCode);
                    country.setCurrencyCode(currencyCode);
                    country.setCurrencyName(currencyName);
                    country.setCurrencySymbol(currencySymbol);
                    country.setCapital(capital);
                    country.setRegion(apiCountry.getRegion());
                    country.setSubregion(apiCountry.getSubregion());
                    country.setIsEnabled(false); // Default disabled
                    countryRepository.save(country);
                    added++;
                }
            }

            log.info("Country sync completed - Added: {}, Updated: {}", added, updated);

            return CountrySyncResponse.builder()
                    .success(true)
                    .message("Countries synced successfully")
                    .addedCount(added)
                    .updatedCount(updated)
                    .totalCount(added + updated)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to sync countries: {}", e.getMessage(), e);
            return CountrySyncResponse.builder()
                    .success(false)
                    .message("Failed to sync countries: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public Page<CountryResponse> searchCountries(String keyword, Pageable pageable) {
        Page<Country> countries;

        if (keyword == null || keyword.trim().isEmpty()) {
            countries = countryRepository.findAll(pageable);
        } else {
            String searchTerm = keyword.trim();
            countries = countryRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    searchTerm, searchTerm, pageable);
        }

        return countries.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CountryResponse toggleCountryStatus(Long id) {
        Country country = countryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found"));

        country.setIsEnabled(!country.getIsEnabled());
        countryRepository.save(country);

        log.info("Country {} has been {}", country.getName(),
                country.getIsEnabled() ? "enabled" : "disabled");

        return mapToResponse(country);
    }

    @Override
    public List<CountryResponse> getEnabledCountries() {
        return countryRepository.findByIsEnabledTrueOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CountryResponse mapToResponse(Country country) {
        return CountryResponse.builder()
                .id(country.getId())
                .name(country.getName())
                .code(country.getCode())
                .phoneCode(country.getPhoneCode())
                .currencyCode(country.getCurrencyCode())
                .currencyName(country.getCurrencyName())
                .currencySymbol(country.getCurrencySymbol())
                .capital(country.getCapital())
                .region(country.getRegion())
                .subregion(country.getSubregion())
                .isEnabled(country.getIsEnabled())
                .build();
    }
}