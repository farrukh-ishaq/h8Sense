package com.islamophobia.detector.ui;

import com.islamophobia.detector.model.entity.ContentAnalysis;
import com.islamophobia.detector.model.entity.UserFeedback;
import com.islamophobia.detector.service.ContentAnalysisService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Main view displaying detected violations with feedback functionality
 */
@Route("")
@PageTitle("Islamophobia Detector - Dashboard")
@RequiredArgsConstructor
public class MainView extends VerticalLayout {

    private final ContentAnalysisService analysisService;

    private Grid<ContentAnalysis> violationsGrid;
    private Div detailPanel;
    private ComboBox<String> categoryFilter;

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent event) {
        super.onAttach(event);
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(createHeader());
        add(createFilters());
        add(createViolationsGrid());
        add(createDetailPanel());

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

    private HorizontalLayout createFilters() {
        categoryFilter = new ComboBox<>("Filter by Category");
        categoryFilter.setItems(
            "ALL",
            "THEOLOGICAL_MISREPRESENTATION",
            "TERRORISM_ASSOCIATION", 
            "CULTURAL_STEREOTYPING",
            "RELIGIOUS_BIAS",
            "HISTORICAL_DISTORTION",
            "OTHER"
        );
        categoryFilter.setValue("ALL");
        categoryFilter.addValueChangeListener(e -> loadViolations());

        Button refreshButton = new Button("⟳ Refresh", e -> loadViolations());

        HorizontalLayout filterLayout = new HorizontalLayout(categoryFilter, refreshButton);
        filterLayout.setAlignItems(Alignment.CENTER);
        return filterLayout;
    }

    private Div createViolationsGrid() {
        violationsGrid = new Grid<>(ContentAnalysis.class, false);
        violationsGrid.addColumn(a -> a.getContentItem().getContent())
            .setHeader("Content")
            .setFlexGrow(2);
        violationsGrid.addColumn(a -> a.getContentItem().getSourcePlatform())
            .setHeader("Platform");
        violationsGrid.addColumn(a -> String.format("%.1f%%", a.getViolationConfidence() * 100))
            .setHeader("Confidence");
        violationsGrid.addColumn(a -> a.getCategory().name())
            .setHeader("Category");
        violationsGrid.addComponentColumn(this::createFeedbackButtons)
            .setHeader("Feedback");

        violationsGrid.addItemClickListener(e -> showDetails(e.getItem()));
        violationsGrid.setSizeFull();
        violationsGrid.setHeight("400px");

        Div gridContainer = new Div(violationsGrid);
        gridContainer.setSizeFull();
        return gridContainer;
    }

    private HorizontalLayout createFeedbackButtons(ContentAnalysis analysis) {
        Button likeBtn = new Button("👍", e -> submitFeedback(analysis, true));
        Button dislikeBtn = new Button("👎", e -> submitFeedback(analysis, false));
        
        likeBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL);
        dislikeBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL);
        
        return new HorizontalLayout(likeBtn, dislikeBtn);
    }

    private Div createDetailPanel() {
        detailPanel = new Div();
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

        violationsGrid.setItems(results.getContent());
        
        Map<String, Long> stats = analysisService.getFeedbackStats();
        Notification.show(String.format("📊 Stats: %d likes, %d dislikes", 
            stats.getOrDefault("likes", 0L), 
            stats.getOrDefault("dislikes", 0L)), 3000, Notification.Position.BOTTOM_END);
    }

    private void showDetails(ContentAnalysis analysis) {
        Div content = new Div();
        content.add(new Paragraph("📝 Content: " + analysis.getContentItem().getContent()));
        content.add(new Paragraph("🏷️ Category: " + analysis.getCategory()));
        content.add(new Paragraph("✅ Confidence: " + String.format("%.1f%%", analysis.getViolationConfidence() * 100)));
        content.add(new Paragraph("📖 Explanation: " + analysis.getViolationExplanation()));
        content.add(new Paragraph("💬 Counter-argument: " + analysis.getCounterArgument()));
        
        if (!analysis.getQuranReferences().isEmpty()) {
            content.add(new Paragraph("📿 Quran References: " + String.join(", ", analysis.getQuranReferences())));
        }
        if (!analysis.getHadithReferences().isEmpty()) {
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
}
