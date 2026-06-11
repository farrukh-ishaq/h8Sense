package com.islamophobia.detector.ui;

import com.islamophobia.detector.model.entity.ContentAnalysis;
import com.islamophobia.detector.model.entity.UserFeedback;
import com.islamophobia.detector.service.ContentAnalysisService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Main view displaying detected violations with feedback functionality
 */
@Route("")
@PageTitle("Islamophobia Detector - Dashboard")
public class MainView extends VerticalLayout {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final ContentAnalysisService analysisService;

    private final Grid<ContentAnalysis> violationsGrid = new Grid<>(ContentAnalysis.class, false);
    private final Div detailPanel = new Div();
    private final ComboBox<String> categoryFilter = new ComboBox<>("Filter by Category");
    private final Paragraph statusMessage = new Paragraph();
    private final Span totalAnalysesValue = new Span("0");
    private final Span confirmedViolationsValue = new Span("0");
    private final Span nonViolationsValue = new Span("0");
    private final Span pendingContentValue = new Span("0");
    private final Span likesValue = new Span("0");
    private final Span dislikesValue = new Span("0");

    public MainView(ContentAnalysisService analysisService) {
        this.analysisService = analysisService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(createHeader());
        add(createSummaryCards());
        add(createFilters());
        add(createViolationsGrid());
        add(createDetailPanel());
        expand(violationsGrid);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        loadViolations();
    }

    private Div createHeader() {
        H1 title = new H1("🛡️ Islamophobia Content Detector");
        title.getStyle().set("margin-top", "0");

        Paragraph subtitle = new Paragraph(
            "AI-powered detection of hate speech against Islam with scholarly responses"
        );
        subtitle.getStyle().set("color", "#666");

        HorizontalLayout headerLayout = new HorizontalLayout(title);
        headerLayout.setAlignItems(Alignment.BASELINE);

        Div headerDiv = new Div(title, subtitle);
        headerDiv.setWidthFull();
        return headerDiv;
    }

    private HorizontalLayout createSummaryCards() {
        HorizontalLayout summaryLayout = new HorizontalLayout(
            createStatCard("Analyses", totalAnalysesValue),
            createStatCard("Confirmed Violations", confirmedViolationsValue),
            createStatCard("Non-Violations", nonViolationsValue),
            createStatCard("Pending Content", pendingContentValue),
            createStatCard("Likes", likesValue),
            createStatCard("Dislikes", dislikesValue)
        );
        summaryLayout.setWidthFull();
        summaryLayout.setSpacing(true);
        return summaryLayout;
    }

    private Div createStatCard(String label, Span value) {
        value.getStyle()
            .set("font-size", "1.5rem")
            .set("font-weight", "700");

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("color", "#666");

        Div card = new Div(value, labelSpan);
        card.getStyle()
            .set("border", "1px solid #e5e7eb")
            .set("border-radius", "10px")
            .set("padding", "12px 16px")
            .set("background", "white")
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("gap", "4px")
            .set("min-width", "150px");
        return card;
    }

    private HorizontalLayout createFilters() {
        categoryFilter.setItems(
            "ALL",
            "DIRECT_INSULT",
            "MISREPRESENTATION",
            "STEREOTYPING",
            "DEHUMANIZATION",
            "INCITEMENT_VIOLENCE",
            "DISCRIMINATION",
            "HISTORICAL_REVISIONISM",
            "CONSPIRACY_THEORY",
            "MOCKERY_PRACTICES",
            "OTHER"
        );
        categoryFilter.setValue("ALL");
        categoryFilter.addValueChangeListener(e -> loadViolations());

        Button refreshButton = new Button("⟳ Refresh", e -> loadViolations());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        statusMessage.getStyle().set("color", "#475569");
        statusMessage.getStyle().set("margin", "0");

        HorizontalLayout filterLayout = new HorizontalLayout(categoryFilter, refreshButton, statusMessage);
        filterLayout.setAlignItems(Alignment.CENTER);
        filterLayout.setWidthFull();
        return filterLayout;
    }

    private Div createViolationsGrid() {
        violationsGrid.addColumn(a -> defaultString(a.getContentItem().getTitle(), "Untitled content"))
            .setHeader("Title")
            .setAutoWidth(true);
        violationsGrid.addColumn(a -> preview(defaultString(a.getContentItem().getContent(), ""), 140))
            .setHeader("Content Preview")
            .setFlexGrow(2);
        violationsGrid.addColumn(a -> a.getContentItem().getSourcePlatform())
            .setHeader("Platform");
        violationsGrid.addColumn(a -> a.isConfirmedViolation() ? "Violation" : "No violation")
            .setHeader("Status")
            .setAutoWidth(true);
        violationsGrid.addColumn(a -> String.format("%.1f%%", a.getViolationConfidence().doubleValue() * 100))
            .setHeader("Confidence");
        violationsGrid.addColumn(a -> a.getCategory() != null ? a.getCategory().name() : "OTHER")
            .setHeader("Category");
        violationsGrid.addColumn(a -> DATE_TIME_FORMATTER.format(a.getAnalyzedAt()))
            .setHeader("Analyzed At")
            .setAutoWidth(true);
        violationsGrid.addComponentColumn(this::createFeedbackButtons)
            .setHeader("Feedback");

        violationsGrid.addItemClickListener(e -> showDetails(e.getItem().getId()));
        violationsGrid.setSizeFull();
        violationsGrid.setHeight("400px");

        Div gridContainer = new Div(violationsGrid);
        gridContainer.setSizeFull();
        return gridContainer;
    }

    private HorizontalLayout createFeedbackButtons(ContentAnalysis analysis) {
        Button likeBtn = new Button("👍", e -> submitFeedback(analysis, true));
        Button dislikeBtn = new Button("👎", e -> submitFeedback(analysis, false));

        likeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        dislikeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

        return new HorizontalLayout(likeBtn, dislikeBtn);
    }

    private Div createDetailPanel() {
        detailPanel.setText("Select a violation to view details");
        detailPanel.getStyle()
            .set("border", "1px solid #ddd")
            .set("padding", "16px")
            .set("border-radius", "4px")
            .set("background-color", "#f9f9f9");
        detailPanel.setWidthFull();
        return detailPanel;
    }

    private void loadViolations() {
        PageRequest pageRequest = PageRequest.of(0, 50, Sort.by("analyzedAt").descending());
        Page<ContentAnalysis> results;

        String selectedCategory = categoryFilter.getValue();
        if ("ALL".equals(selectedCategory)) {
            results = analysisService.getViolations(pageRequest);
        } else {
            results = analysisService.getViolationsByCategory(selectedCategory, pageRequest);
        }

        boolean showingFallback = results.isEmpty();
        if (showingFallback) {
            results = analysisService.getRecentAnalyses(pageRequest);
            statusMessage.setText(results.isEmpty()
                ? "No analyses available yet. Background processing may still be running."
                : "No confirmed violations yet. Showing recent analyses instead.");
        } else {
            statusMessage.setText("Showing confirmed violations.");
        }

        violationsGrid.setItems(results.getContent());

        updateDashboardStats();
    }

    private void updateDashboardStats() {
        Map<String, Long> dashboardStats = analysisService.getDashboardStats();
        Map<String, Long> stats = analysisService.getFeedbackStats();
        totalAnalysesValue.setText(String.valueOf(dashboardStats.getOrDefault("totalAnalyses", 0L)));
        confirmedViolationsValue.setText(String.valueOf(dashboardStats.getOrDefault("confirmedViolations", 0L)));
        nonViolationsValue.setText(String.valueOf(dashboardStats.getOrDefault("nonViolations", 0L)));
        pendingContentValue.setText(String.valueOf(dashboardStats.getOrDefault("pendingContent", 0L)));
        likesValue.setText(String.valueOf(stats.getOrDefault("likes", 0L)));
        dislikesValue.setText(String.valueOf(stats.getOrDefault("dislikes", 0L)));
    }

    private void showDetails(java.util.UUID analysisId) {
        ContentAnalysis analysis = analysisService.getAnalysisDetails(analysisId)
            .orElse(null);

        if (analysis == null) {
            detailPanel.removeAll();
            detailPanel.add(new Paragraph("Unable to load analysis details."));
            return;
        }

        Div content = new Div();
        content.add(new Paragraph("📰 Title: " + defaultString(analysis.getContentItem().getTitle(), "Untitled content")));
        content.add(new Paragraph("📝 Content: " + defaultString(analysis.getContentItem().getContent(), "")));
        content.add(new Paragraph("📍 Platform: " + defaultString(analysis.getContentItem().getSourcePlatform(), "Unknown")));
        content.add(new Paragraph("🏷️ Category: " + (analysis.getCategory() != null ? analysis.getCategory() : "OTHER")));
        content.add(new Paragraph("🚦 Status: " + (analysis.isConfirmedViolation() ? "Confirmed violation" : "No violation detected")));
        content.add(new Paragraph("✅ Confidence: " + String.format("%.1f%%", analysis.getViolationConfidence().doubleValue() * 100)));
        content.add(new Paragraph("📖 Explanation: " + defaultString(analysis.getViolationExplanation(), "No explanation available.")));
        content.add(new Paragraph("💬 Counter-argument: " + defaultString(analysis.getCounterArgument(), "No counter-argument available.")));
        if (analysis.getContentItem().getSourceUrl() != null && !analysis.getContentItem().getSourceUrl().isBlank()) {
            content.add(new Anchor(analysis.getContentItem().getSourceUrl(), "🔗 Open source article"));
        }

        if (analysis.getQuranReferences() != null && !analysis.getQuranReferences().isEmpty()) {
            content.add(new Paragraph("📿 Quran References: " + String.join(", ", analysis.getQuranReferences())));
        }
        if (analysis.getHadithReferences() != null && !analysis.getHadithReferences().isEmpty()) {
            content.add(new Paragraph("📚 Hadith References: " + String.join(", ", analysis.getHadithReferences())));
        }

        detailPanel.removeAll();
        detailPanel.add(content);
    }

    private void submitFeedback(ContentAnalysis analysis, boolean isPositive) {
        try {
            String ipAddress = "127.0.0.1"; // Local testing
            String deviceFingerprint = "vaadin-ui";
            String userAgent = "Vaadin UI";
            String comment = null;

            analysisService.submitFeedback(
                analysis.getId(),
                isPositive ? UserFeedback.FeedbackType.LIKE : UserFeedback.FeedbackType.DISLIKE,
                ipAddress,
                deviceFingerprint,
                userAgent,
                comment
            );
            Notification.show(isPositive ? "✅ Thank you for your feedback!" : "❌ Feedback recorded", 2000, Notification.Position.MIDDLE);
            loadViolations();
        } catch (Exception e) {
            Notification.show("⚠️ Error submitting feedback: " + e.getMessage(), 2000, Notification.Position.MIDDLE);
        }
    }

    private String preview(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
