package com.islamophobia.detector.ui.components;

import com.vaadin.flow.component.html.Span;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DashboardViewSupport {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private DashboardViewSupport() {
    }

    public static String preview(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }

    public static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static String formatInstant(Instant instant) {
        return instant == null ? "n/a" : DATE_TIME_FORMATTER.format(instant);
    }

    public static String formatConfidence(BigDecimal confidence) {
        BigDecimal safe = confidence == null ? BigDecimal.ZERO : confidence;
        return String.format("%.1f%%", safe.doubleValue() * 100);
    }

    public static Span createBadge(String text, String variantClass) {
        Span badge = new Span(text);
        badge.addClassNames("badge", variantClass);
        return badge;
    }

    public static Span createMutedCaption(String text) {
        Span span = new Span(text);
        span.addClassName("muted-caption");
        return span;
    }

    public static Span createEmphasisValue(String text) {
        Span span = new Span(text);
        span.addClassName("emphasis-value");
        return span;
    }
}

