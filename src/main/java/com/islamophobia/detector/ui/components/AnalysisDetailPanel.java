package com.islamophobia.detector.ui.components;

import com.islamophobia.detector.model.entity.ContentAnalysis;
import com.islamophobia.detector.model.entity.ContentItem;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;
import java.util.Map;

public class AnalysisDetailPanel extends Div {

    public AnalysisDetailPanel() {
        addClassNames("dashboard-card", "dashboard-detail-panel");
        setWidthFull();
        showPlaceholder();
    }

    public void showPlaceholder() {
        removeAll();

        Div placeholder = new Div();
        placeholder.addClassName("detail-placeholder");
        placeholder.add(
            new H3("Analysis Inspector"),
            new Paragraph("Select a row to inspect the full source record, references, feedback totals, model metadata, and raw response payload.")
        );

        add(placeholder);
    }

    public void showAnalysis(ContentAnalysis analysis, Map<String, Long> feedbackStats) {
        removeAll();

        ContentItem item = analysis.getContentItem();
        if (item == null) {
            showPlaceholder();
            return;
        }

        VerticalLayout content = new VerticalLayout();
        content.addClassName("detail-layout");
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        content.add(createDetailHero(analysis, item));
        content.add(createSectionCard("Source Context", createSourceContextLayout(item)));
        content.add(createSectionCard("Analysis", createAnalysisLayout(analysis, feedbackStats)));
        content.add(createSectionCard("References", createReferencesLayout(analysis)));

        if (item.getMetadata() != null && !item.getMetadata().isEmpty()) {
            content.add(createSectionCard("Content Metadata", createMetadataLayout(item.getMetadata())));
        }

        Details rawResponse = new Details("Raw LLM Response", createRawResponseLayout(analysis));
        rawResponse.addClassName("detail-raw-response");
        rawResponse.setOpened(false);
        content.add(rawResponse);

        add(content);
    }

    private Component createDetailHero(ContentAnalysis analysis, ContentItem item) {
        Div hero = new Div();
        hero.addClassName("detail-hero");

        VerticalLayout copy = new VerticalLayout();
        copy.setPadding(false);
        copy.setSpacing(false);
        copy.addClassName("detail-hero__copy");

        H3 title = new H3(DashboardViewSupport.defaultString(item.getTitle(), "Untitled content"));
        Paragraph preview = new Paragraph(DashboardViewSupport.preview(DashboardViewSupport.defaultString(item.getContent(), ""), 260));
        preview.addClassName("detail-hero__preview");
        copy.add(title, preview);

        HorizontalLayout badges = new HorizontalLayout(
            createStatusBadge(analysis),
            DashboardViewSupport.createBadge(DashboardViewSupport.formatConfidence(analysis.getViolationConfidence()), "badge--confidence"),
            DashboardViewSupport.createBadge(DashboardViewSupport.defaultString(analysis.getModelUsed(), "Unknown model"), "badge--model")
        );
        badges.addClassName("detail-hero__badges");
        badges.setSpacing(true);

        hero.add(copy, badges);
        return hero;
    }

    private Component createSourceContextLayout(ContentItem item) {
        VerticalLayout layout = new VerticalLayout();
        layout.addClassName("detail-section-layout");
        layout.setPadding(false);
        layout.setSpacing(false);

        layout.add(
            createKeyValueRow("Platform", DashboardViewSupport.defaultString(item.getSourcePlatform(), "Unknown")),
            createKeyValueRow("Source Type", item.getSourceType() != null ? item.getSourceType().name() : "OTHER"),
            createKeyValueRow("Author", DashboardViewSupport.defaultString(item.getAuthor(), "Unknown")),
            createKeyValueRow("Created", DashboardViewSupport.formatInstant(item.getCreatedAt())),
            createKeyValueRow("Detected", DashboardViewSupport.formatInstant(item.getDetectedAt())),
            createKeyValueRow("Source Label", DashboardViewSupport.defaultString(item.getSource(), "Not provided"))
        );

        if (item.getSourceUrl() != null && !item.getSourceUrl().isBlank()) {
            Anchor link = new Anchor(item.getSourceUrl(), item.getSourceUrl());
            link.setTarget("_blank");
            layout.add(createKeyValueRow("Source URL", link));
        }

        layout.add(createKeyValueRow("Content", DashboardViewSupport.preview(DashboardViewSupport.defaultString(item.getContent(), ""), 1200)));
        return layout;
    }

    private Component createAnalysisLayout(ContentAnalysis analysis, Map<String, Long> feedbackStats) {
        VerticalLayout layout = new VerticalLayout();
        layout.addClassName("detail-section-layout");
        layout.setPadding(false);
        layout.setSpacing(false);

        layout.add(
            createKeyValueRow("Status", analysis.isConfirmedViolation() ? "Confirmed violation" : "No violation detected"),
            createKeyValueRow("Category", analysis.getCategory() != null ? analysis.getCategory().name() : "OTHER"),
            createKeyValueRow("Confidence", DashboardViewSupport.formatConfidence(analysis.getViolationConfidence())),
            createKeyValueRow("Model Used", DashboardViewSupport.defaultString(analysis.getModelUsed(), "Unknown")),
            createKeyValueRow("Tokens Used", analysis.getTokensUsed() != null ? analysis.getTokensUsed().toString() : "n/a"),
            createKeyValueRow("Explanation", DashboardViewSupport.defaultString(analysis.getViolationExplanation(), "No explanation available.")),
            createKeyValueRow("Counter-Argument", DashboardViewSupport.defaultString(analysis.getCounterArgument(), "No counter-argument available.")),
            createKeyValueRow("Feedback", String.format(
                "%d likes / %d dislikes",
                feedbackStats.getOrDefault("likes", 0L),
                feedbackStats.getOrDefault("dislikes", 0L)
            ))
        );
        return layout;
    }

    private Component createReferencesLayout(ContentAnalysis analysis) {
        Div wrapper = new Div();
        wrapper.addClassName("reference-wrapper");
        wrapper.add(
            createReferenceBlock("Quran References", analysis.getQuranReferences(), "badge--success"),
            createReferenceBlock("Hadith References", analysis.getHadithReferences(), "badge--warning")
        );
        return wrapper;
    }

    private Component createMetadataLayout(Map<String, String> metadata) {
        VerticalLayout layout = new VerticalLayout();
        layout.addClassName("detail-section-layout");
        layout.setPadding(false);
        layout.setSpacing(false);

        metadata.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> layout.add(createKeyValueRow(entry.getKey(), entry.getValue())));
        return layout;
    }

    private Component createRawResponseLayout(ContentAnalysis analysis) {
        Pre raw = new Pre(DashboardViewSupport.defaultString(analysis.getLlmRawResponse(), "No raw response stored."));
        raw.addClassName("raw-response");
        return raw;
    }

    private Div createSectionCard(String title, Component content) {
        Div section = new Div();
        section.addClassName("detail-section-card");

        H3 heading = new H3(title);
        heading.addClassName("section-title");

        section.add(heading, content);
        return section;
    }

    private Div createKeyValueRow(String label, String value) {
        return createKeyValueRow(label, new Span(DashboardViewSupport.defaultString(value, "-")));
    }

    private Div createKeyValueRow(String label, Component valueComponent) {
        Div row = new Div();
        row.addClassName("kv-row");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("kv-row__label");

        valueComponent.getElement().getClassList().add("kv-row__value");
        row.add(labelSpan, valueComponent);
        return row;
    }

    private Component createReferenceBlock(String title, List<String> references, String badgeVariant) {
        Div block = new Div();
        block.addClassName("reference-block");

        Span heading = new Span(title);
        heading.addClassName("reference-block__title");
        block.add(heading);

        if (references == null || references.isEmpty()) {
            block.add(DashboardViewSupport.createMutedCaption("No references stored."));
            return block;
        }

        HorizontalLayout chips = new HorizontalLayout();
        chips.addClassName("reference-chips");
        chips.setSpacing(true);
        references.forEach(reference -> chips.add(DashboardViewSupport.createBadge(reference, badgeVariant)));
        block.add(chips);
        return block;
    }

    private Span createStatusBadge(ContentAnalysis analysis) {
        return analysis.isConfirmedViolation()
            ? DashboardViewSupport.createBadge("Confirmed violation", "badge--danger")
            : DashboardViewSupport.createBadge("No violation", "badge--success");
    }
}

