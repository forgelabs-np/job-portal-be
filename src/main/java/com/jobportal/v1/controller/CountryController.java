package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.country.response.CountryResponse;
import com.jobportal.v1.dto.country.response.CountrySyncResponse;
import com.jobportal.v1.service.CountrySyncService;
import com.jobportal.v1.util.Pages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/countries")
@Tag(name = "Country Management", description = "Country Management APIs")
public class CountryController {

    private final CountrySyncService countrySyncService;

    @Operation(summary = "Sync Countries", description = "Fetch all countries from REST API and save/update to database")
    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CountrySyncResponse>> syncCountries() {
        CountrySyncResponse response = countrySyncService.syncCountriesFromApi();
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @Operation(summary = "Search Countries", description = "Search countries by name or code (Admin only)")
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageRes<CountryResponse>>> searchCountries(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = Pages.toPageable(page, size);
        Page<CountryResponse> countryPage = countrySyncService.searchCountries(keyword, pageable);
        PageRes<CountryResponse> response = Pages.of(countryPage);

        return ResponseEntity.ok(ApiResponse.success("Countries retrieved", response));
    }

    @Operation(summary = "Toggle Country Status", description = "Enable or disable a country (Admin only)")
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CountryResponse>> toggleCountryStatus(@PathVariable Long id) {
        CountryResponse response = countrySyncService.toggleCountryStatus(id);
        String message = response.getIsEnabled() ? "Country enabled successfully" : "Country disabled successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Get Enabled Countries", description = "Get all enabled countries (Public - for job posting)")
    @GetMapping("/enabled")
    public ResponseEntity<ApiResponse<List<CountryResponse>>> getEnabledCountries() {
        List<CountryResponse> response = countrySyncService.getEnabledCountries();
        return ResponseEntity.ok(ApiResponse.success("Enabled countries retrieved", response));
    }
}