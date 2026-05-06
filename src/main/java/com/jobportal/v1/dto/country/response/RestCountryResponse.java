package com.jobportal.v1.dto.country.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestCountryResponse {
    private Name name;
    private String cca2; // country code (2 letter)
    private Map<String, Currency> currencies;
    private String[] capital;
    private String region;
    private String subregion;
    private String[] callingCodes;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Name {
        private String common;
        private String official;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Currency {
        private String name;
        private String symbol;
    }
}