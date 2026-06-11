package com.islamophobia.detector.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class DashboardSummaryPanel extends Div {

    private final Span totalAnalysesValue = new Span("0");
    private final Span confirmedViolationsValue = new Span("0");
    private final Span nonViolationsValue = new Span("0");
    private final Span pendingContentValue = new Span("0");
    private final Span visibleRowsValue = new Span("0");
    private final Span avgConfidenceValue = new Span("0%");
    private final Span coveredPlatformsValue = new Span("0");
    private final Span likesValue = new Span("0");
    private final Span dislikesValue = new Span("0");

    public DashboardSummaryPanel() {
        addClassName("dashboard-summary-grid");
        add(
            createStatCard("Total Analyses", totalAnalysesValue, "All stored AI analysis records."),
            createStatCard("Confirmed Violations", confirmedViolationsValue, "Rows marked as violations."),
            createStatCard("Non-Violations", nonViolationsValue, "Useful for reviewing model precision."),
            createStatCard("Pending Content", pendingContentValue, "Items still waiting for analysis."),
            createStatCard("Visible Rows", visibleRowsValue, "Rows matching current filters."),
            createStatCard("Avg Confidence", avgConfidenceValue, "Average confidence of visible analyses."),
            createStatCard("Platforms", coveredPlatformsValue, "Distinct visible platforms."),
            createFeedbackCard()
        );
    }

    public void update(long totalAnalyses,
                       long confirmedViolations,
                       long nonViolations,
                       long pendingContent,
                       int visibleRows,
                       String averageConfidence,
                       long coveredPlatforms,
                       long likes,
                       long dislikes) {
        totalAnalysesValue.setText(String.valueOf(totalAnalyses));
        confirmedViolationsValue.setText(String.valueOf(confirmedViolations));
        nonViolationsValue.setText(String.valueOf(nonViolations));
        pendingContentValue.setText(String.valueOf(pendingContent));
        visibleRowsValue.setText(String.valueOf(visibleRows));
        avgConfidenceValue.setText(averageConfidence);
        coveredPlatformsValue.setText(String.valueOf(coveredPlatforms));
        likesValue.setText(String.valueOf(likes));
        dislikesValue.setText(String.valueOf(dislikes));
    }

    private Div createStatCard(String label, Span value, String helperText) {
        Div card = new Div();
        card.addClassNames("dashboard-card", "stat-card");

        value.addClassName("stat-card__value");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("stat-card__label");

        Paragraph helper = new Paragraph(helperText);
        helper.addClassName("stat-card__helper");

        card.add(value, labelSpan, helper);
        return card;
    }

    private Div createFeedbackCard() {
        Div card = new Div();
        card.addClassNames("dashboard-card", "stat-card");

        HorizontalLayout row = new HorizontalLayout(
            createValuePair("👍", likesValue, "accent-success"),
            createValuePair("👎", dislikesValue, "accent-danger")
        );
        row.addClassName("feedback-summary");
        row.setSpacing(true);

        Span label = new Span("Feedback");
        label.addClassName("stat-card__label");

        Paragraph helper = new Paragraph("Total likes and dislikes across all stored analyses.");
        helper.addClassName("stat-card__helper");

        card.add(row, label, helper);
        return card;
    }

    private Div createValuePair(String prefix, Span value, String accentClass) {
        Div wrapper = new Div();
        wrapper.addClassNames("feedback-pair", accentClass);

        Span prefixSpan = new Span(prefix);
        prefixSpan.addClassName("feedback-pair__icon");
        value.addClassName("feedback-pair__value");

        wrapper.add(prefixSpan, value);
        return wrapper;
    }
}

