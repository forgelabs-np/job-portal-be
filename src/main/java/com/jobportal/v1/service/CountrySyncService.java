package com.jobportal.v1.service;

import com.jobportal.v1.dto.country.response.CountryResponse;
import com.jobportal.v1.dto.country.response.CountrySyncResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CountrySyncService {

    CountrySyncResponse syncCountriesFromApi();

    Page<CountryResponse> searchCountries(String keyword, Pageable pageable);

    CountryResponse toggleCountryStatus(Long id);

    List<CountryResponse> getEnabledCountries();
}