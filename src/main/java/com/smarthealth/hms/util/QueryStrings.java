package com.smarthealth.hms.util;

import org.springframework.web.util.UriComponentsBuilder;

/** Builds an encoded query string (without leading '?') from key/value pairs, skipping blank values. Used by pagination links. */
public final class QueryStrings {

    private QueryStrings() {
    }

    public static String of(Object... keyValues) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            if (value != null && !value.toString().isBlank()) {
                builder.queryParam(String.valueOf(keyValues[i]), value);
            }
        }
        String query = builder.build().encode().getQuery();
        return query == null ? "" : query;
    }
}
