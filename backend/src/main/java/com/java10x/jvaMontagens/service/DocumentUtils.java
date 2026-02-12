package com.java10x.jvaMontagens.service;

public final class DocumentUtils {
    private DocumentUtils() {
    }

    public static String normalizeCpf(String cpf) {
        return normalizeDocument(cpf, 11, "CPF");
    }

    public static String normalizeCnpj(String cnpj) {
        return normalizeDocument(cnpj, 14, "CNPJ");
    }

    private static String normalizeDocument(String value, int expectedLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        String digitsOnly = value.replaceAll("\\D", "");
        if (digitsOnly.length() != expectedLength) {
            throw new IllegalArgumentException(fieldName + " must contain " + expectedLength + " digits.");
        }

        return digitsOnly;
    }
}
