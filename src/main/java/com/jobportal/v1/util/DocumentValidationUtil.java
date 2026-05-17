package com.jobportal.v1.util;

import com.jobportal.v1.enums.DocumentType;
import com.jobportal.v1.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class DocumentValidationUtil {

    public static DocumentType validateAndGetDocumentType(String documentType) {
        if (documentType == null || documentType.trim().isEmpty()) {
            throw new BadRequestException("Document type is required. Allowed types: " + getValidDocumentTypes());
        }

        String trimmedType = documentType.toUpperCase().trim();

        try {
            return DocumentType.valueOf(trimmedType);
        } catch (IllegalArgumentException e) {
            String validTypes = getValidDocumentTypes();

            log.warn("Invalid document type attempted: '{}'. Valid types: {}", documentType, validTypes);

            throw new BadRequestException(
                    String.format("Invalid document type: '%s'. Allowed types are: %s",
                            documentType, validTypes)
            );
        }
    }

    private static String getValidDocumentTypes() {
        return Arrays.stream(DocumentType.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}