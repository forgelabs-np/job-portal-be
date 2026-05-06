package com.jobportal.v1.dto.country.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CountryResponse {
    private Long id;
    private String name;
    private String code;
    private String phoneCode;
    private String currencyCode;
    private String currencyName;
    private String currencySymbol;
    private String capital;
    private String region;
    private String subregion;
    private Boolean isEnabled;
}