package com.islamophobia.detector.ui;

import com.islamophobia.detector.model.entity.ContentAnalysis;
import com.islamophobia.detector.model.entity.ContentItem;
import com.islamophobia.detector.model.entity.UserFeedback;
import com.islamophobia.detector.service.ContentAnalysisService;
import com.islamophobia.detector.ui.components.AnalysisDetailPanel;
import com.islamophobia.detector.ui.components.DashboardFilterPanel;
import com.islamophobia.detector.ui.components.DashboardFooter;
import com.islamophobia.detector.ui.components.DashboardHeader;
import com.islamophobia.detector.ui.components.DashboardSummaryPanel;
import com.islamophobia.detector.ui.components.DashboardViewSupport;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Dashboard view composed of a stable header/footer and a richer body with filters, grid, and details.
 */
@Route("")
@PageTitle("Islamophobia Detector - Dashboard")
public class MainView extends VerticalLayout {

    private static final String FILTER_ALL = "ALL";
    private static final String FILTER_VIOLATIONS = "VIOLATIONS_ONLY";
    private static final String FILTER_NON_VIOLATIONS = "NON_VIOLATIONS_ONLY";

    private final ContentAnalysisService analysisService;

    private final Grid<ContentAnalysis> analysesGrid = new Grid<>(ContentAnalysis.class, false);
    private final AnalysisDetailPanel detailPanel = new AnalysisDetailPanel();
    private final DashboardSummaryPanel summaryPanel = new DashboardSummaryPanel();
    private final DashboardFilterPanel filterPanel = new DashboardFilterPanel(
        this::refreshDashboardData,
        this::showPlaceholderDetails,
        this::onFilterChanged
    );

    private List<ContentAnalysis> allAnalyses = List.of();
    private List<ContentAnalysis> filteredAnalyses = List.of();
    private UUID selectedAnalysisId;
    private boolean suppressFilterEvents;

    public MainView(ContentAnalysisService analysisService) {
        this.analysisService = analysisService;

        addClassName("app-shell");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        configureFilters();
        configureGrid();

        Div header = createHeaderSection();
        Div body = createBodySection();
        Div footer = createFooterSection();

        add(header, body, footer);
        expand(body);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        refreshDashboardData();
    }

    private void configureFilters() {
        suppressFilterEvents = true;

        filterPanel.getStatusFilter().setItems(FILTER_ALL, FILTER_VIOLATIONS, FILTER_NON_VIOLATIONS);
        filterPanel.getStatusFilter().setValue(FILTER_ALL);

        filterPanel.getCategoryFilter().setItems(
            FILTER_ALL,
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
        filterPanel.getCategoryFilter().setValue(FILTER_ALL);

        filterPanel.getPlatformFilter().setItems(FILTER_ALL);
        filterPanel.getPlatformFilter().setValue(FILTER_ALL);

        suppressFilterEvents = false;
    }

    private void configureGrid() {
        analysesGrid.addClassName("dashboard-grid");
        analysesGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        analysesGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        analysesGrid.addComponentColumn(this::createContentCell)
            .setHeader("Content & Source")
            .setFlexGrow(2)
            .setAutoWidth(true);

        analysesGrid.addComponentColumn(this::createStatusCell)
            .setHeader("Status")
            .setFlexGrow(0)
            .setAutoWidth(true);

        analysesGrid.addComponentColumn(this::createConfidenceCell)
            .setHeader("Confidence & Timing")
            .setFlexGrow(0)
            .setAutoWidth(true);

        analysesGrid.addComponentColumn(this::createEvidenceCell)
            .setHeader("Evidence & Model")
            .setFlexGrow(1)
            .setAutoWidth(true);

        analysesGrid.addComponentColumn(this::createFeedbackButtons)
            .setHeader("Feedback")
            .setFlexGrow(0)
            .setAutoWidth(true);

        analysesGrid.asSingleSelect().addValueChangeListener(event -> {
            ContentAnalysis analysis = event.getValue();
            if (analysis == null) {
                showPlaceholderDetails();
                return;
            }
            selectedAnalysisId = analysis.getId();
            showDetails(analysis.getId());
        });
    }

    private Div createHeaderSection() {
        return new DashboardHeader();
    }

    private Div createBodySection() {
        Div body = new Div();
        body.addClassName("app-body");

        body.add(
            summaryPanel,
            filterPanel,
            createContentSection()
        );
        return body;
    }

    private Div createFooterSection() {
        return new DashboardFooter();
    }

    private Component createContentSection() {
        Div leftPane = new Div();
        leftPane.addClassNames("dashboard-card", "dashboard-pane");
        leftPane.add(new H2("Recent Analyses"), analysesGrid);

        Div rightPane = new Div();
        rightPane.addClassNames("dashboard-card", "dashboard-pane");
        rightPane.add(new H2("Detail Inspector"), detailPanel);

        SplitLayout splitLayout = new SplitLayout(leftPane, rightPane);
        splitLayout.addClassName("dashboard-split");
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(58);
        return splitLayout;
    }

    private void onFilterChanged() {
        if (suppressFilterEvents) {
            return;
        }
        applyFilters();
    }

    private void refreshDashboardData() {
        allAnalyses = analysisService.getRecentAnalyses(PageRequest.of(0, 100, Sort.by("analyzedAt").descending())).getContent();
        updatePlatformFilterOptions();
        applyFilters();
    }

    private void applyFilters() {
        filteredAnalyses = allAnalyses.stream()
            .filter(this::matchesStatus)
            .filter(this::matchesCategory)
            .filter(this::matchesPlatform)
            .toList();

        analysesGrid.setItems(filteredAnalyses);
        updateDashboardStats();
        updateStatusMessage();
        syncSelectionAndDetails();
    }

    private void updatePlatformFilterOptions() {
        String currentValue = filterPanel.getPlatformFilter().getValue();
        List<String> options = Stream.concat(
                Stream.of(FILTER_ALL),
                allAnalyses.stream()
                    .map(ContentAnalysis::getContentItem)
                    .filter(Objects::nonNull)
                    .map(ContentItem::getSourcePlatform)
                    .filter(platform -> platform != null && !platform.isBlank())
                    .distinct()
                    .sorted()
            )
            .toList();

        suppressFilterEvents = true;
        try {
            filterPanel.getPlatformFilter().setItems(options);
            filterPanel.getPlatformFilter().setValue(options.contains(currentValue) ? currentValue : FILTER_ALL);
        } finally {
            suppressFilterEvents = false;
        }
    }

    private void updateDashboardStats() {
        Map<String, Long> dashboardStats = analysisService.getDashboardStats();
        Map<String, Long> feedbackStats = analysisService.getFeedbackStats();

        BigDecimal totalConfidence = filteredAnalyses.stream()
            .map(ContentAnalysis::getViolationConfidence)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = filteredAnalyses.isEmpty()
            ? BigDecimal.ZERO
            : totalConfidence.divide(BigDecimal.valueOf(filteredAnalyses.size()), 3, RoundingMode.HALF_UP);

        long platforms = filteredAnalyses.stream()
            .map(ContentAnalysis::getContentItem)
            .filter(Objects::nonNull)
            .map(ContentItem::getSourcePlatform)
            .filter(platform -> platform != null && !platform.isBlank())
            .distinct()
            .count();

        summaryPanel.update(
            dashboardStats.getOrDefault("totalAnalyses", 0L),
            dashboardStats.getOrDefault("confirmedViolations", 0L),
            dashboardStats.getOrDefault("nonViolations", 0L),
            dashboardStats.getOrDefault("pendingContent", 0L),
            filteredAnalyses.size(),
            DashboardViewSupport.formatConfidence(average),
            platforms,
            feedbackStats.getOrDefault("likes", 0L),
            feedbackStats.getOrDefault("dislikes", 0L)
        );
    }

    private void updateStatusMessage() {
        if (allAnalyses.isEmpty()) {
            filterPanel.setStatusMessage("No analyses are available yet. Background ingestion or manual analysis may still be pending.");
            return;
        }
        if (filteredAnalyses.isEmpty()) {
            filterPanel.setStatusMessage("No rows match the current filters. Adjust status, category, or platform filters.");
            return;
        }

        long visibleViolations = filteredAnalyses.stream().filter(ContentAnalysis::isConfirmedViolation).count();
        filterPanel.setStatusMessage(String.format(
            "Showing %d analyses (%d confirmed violations) from the latest %d records.",
            filteredAnalyses.size(),
            visibleViolations,
            allAnalyses.size()
        ));
    }

    private void syncSelectionAndDetails() {
        if (filteredAnalyses.isEmpty()) {
            showPlaceholderDetails();
            return;
        }

        ContentAnalysis selected = filteredAnalyses.stream()
            .filter(analysis -> analysis.getId().equals(selectedAnalysisId))
            .findFirst()
            .orElse(filteredAnalyses.get(0));

        analysesGrid.select(selected);
        showDetails(selected.getId());
    }

    private boolean matchesStatus(ContentAnalysis analysis) {
        String selected = filterPanel.getStatusFilter().getValue();
        if (selected == null || FILTER_ALL.equals(selected)) {
            return true;
        }
        if (FILTER_VIOLATIONS.equals(selected)) {
            return analysis.isConfirmedViolation();
        }
        if (FILTER_NON_VIOLATIONS.equals(selected)) {
            return !analysis.isConfirmedViolation();
        }
        return true;
    }

    private boolean matchesCategory(ContentAnalysis analysis) {
        String selected = filterPanel.getCategoryFilter().getValue();
        if (selected == null || FILTER_ALL.equals(selected)) {
            return true;
        }
        return analysis.getCategory() != null && selected.equalsIgnoreCase(analysis.getCategory().name());
    }

    private boolean matchesPlatform(ContentAnalysis analysis) {
        String selected = filterPanel.getPlatformFilter().getValue();
        if (selected == null || FILTER_ALL.equals(selected)) {
            return true;
        }
        ContentItem item = analysis.getContentItem();
        return item != null && selected.equalsIgnoreCase(DashboardViewSupport.defaultString(item.getSourcePlatform(), ""));
    }

    private void showDetails(UUID analysisId) {
        ContentAnalysis analysis = analysisService.getAnalysisDetails(analysisId).orElse(null);
        if (analysis == null) {
            showPlaceholderDetails();
            return;
        }

        selectedAnalysisId = analysisId;
        Map<String, Long> feedbackStats = analysisService.getFeedbackStats(analysisId);
        detailPanel.showAnalysis(analysis, feedbackStats);
    }

    private void showPlaceholderDetails() {
        selectedAnalysisId = null;
        detailPanel.showPlaceholder();
        analysesGrid.deselectAll();
    }

    private Component createContentCell(ContentAnalysis analysis) {
        ContentItem item = analysis.getContentItem();

        Div cell = new Div();
        cell.addClassName("grid-cell-stack");

        Span title = new Span(DashboardViewSupport.defaultString(item.getTitle(), "Untitled content"));
        title.addClassName("grid-cell-title");

        Paragraph preview = new Paragraph(DashboardViewSupport.preview(DashboardViewSupport.defaultString(item.getContent(), ""), 150));
        preview.addClassName("grid-cell-preview");

        HorizontalLayout badges = new HorizontalLayout(
            DashboardViewSupport.createBadge(DashboardViewSupport.defaultString(item.getSourcePlatform(), "Unknown platform"), "badge--info"),
            DashboardViewSupport.createBadge(item.getSourceType() != null ? item.getSourceType().name() : "OTHER", "badge--neutral")
        );
        badges.addClassName("grid-cell-badges");
        badges.setSpacing(true);

        Span author = new Span("Author: " + DashboardViewSupport.defaultString(item.getAuthor(), "Unknown"));
        author.addClassName("grid-cell-meta");

        cell.add(title, preview, badges, author);
        return cell;
    }

    private Component createStatusCell(ContentAnalysis analysis) {
        Div cell = new Div();
        cell.addClassName("grid-cell-stack");
        cell.add(
            createStatusBadge(analysis),
            DashboardViewSupport.createMutedCaption(analysis.getCategory() != null ? analysis.getCategory().name() : "OTHER")
        );
        return cell;
    }

    private Component createConfidenceCell(ContentAnalysis analysis) {
        Div cell = new Div();
        cell.addClassName("grid-cell-stack");
        cell.add(
            DashboardViewSupport.createEmphasisValue(DashboardViewSupport.formatConfidence(analysis.getViolationConfidence())),
            DashboardViewSupport.createMutedCaption("Analyzed: " + DashboardViewSupport.formatInstant(analysis.getAnalyzedAt())),
            DashboardViewSupport.createMutedCaption("Detected: " + DashboardViewSupport.formatInstant(analysis.getContentItem().getDetectedAt()))
        );
        return cell;
    }

    private Component createEvidenceCell(ContentAnalysis analysis) {
        Div cell = new Div();
        cell.addClassName("grid-cell-stack");
        cell.add(
            DashboardViewSupport.createMutedCaption("Quran refs: " + analysis.getQuranReferences().size()),
            DashboardViewSupport.createMutedCaption("Hadith refs: " + analysis.getHadithReferences().size()),
            DashboardViewSupport.createMutedCaption("Tokens: " + (analysis.getTokensUsed() != null ? analysis.getTokensUsed() : "n/a")),
            DashboardViewSupport.createMutedCaption("Model: " + DashboardViewSupport.defaultString(analysis.getModelUsed(), "Unknown"))
        );

        if (analysis.getContentItem().getSourceUrl() != null && !analysis.getContentItem().getSourceUrl().isBlank()) {
            Anchor sourceLink = new Anchor(analysis.getContentItem().getSourceUrl(), "Open source");
            sourceLink.setTarget("_blank");
            sourceLink.addClassName("source-link");
            cell.add(sourceLink);
        }
        return cell;
    }

    private HorizontalLayout createFeedbackButtons(ContentAnalysis analysis) {
        Button likeBtn = new Button("👍", event -> submitFeedback(analysis, true));
        Button dislikeBtn = new Button("👎", event -> submitFeedback(analysis, false));

        likeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        dislikeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

        HorizontalLayout actions = new HorizontalLayout(likeBtn, dislikeBtn);
        actions.addClassName("feedback-actions");
        actions.setSpacing(false);
        return actions;
    }

    private Span createStatusBadge(ContentAnalysis analysis) {
        return analysis.isConfirmedViolation()
            ? DashboardViewSupport.createBadge("Confirmed violation", "badge--danger")
            : DashboardViewSupport.createBadge("No violation", "badge--success");
    }

    private void submitFeedback(ContentAnalysis analysis, boolean isPositive) {
        try {
            analysisService.submitFeedback(
                analysis.getId(),
                isPositive ? UserFeedback.FeedbackType.LIKE : UserFeedback.FeedbackType.DISLIKE,
                "127.0.0.1",
                "vaadin-ui",
                "Vaadin UI",
                null
            );
            Notification.show(
                isPositive ? "✅ Thank you for your feedback!" : "❌ Feedback recorded",
                2000,
                Notification.Position.MIDDLE
            );
            refreshDashboardData();
        } catch (Exception ex) {
            Notification.show("⚠️ Error submitting feedback: " + ex.getMessage(), 2500, Notification.Position.MIDDLE);
        }
    }
}
