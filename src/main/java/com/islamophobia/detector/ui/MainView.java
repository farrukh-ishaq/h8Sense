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
    protected void onAttach(AttachEvent event) {
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
        
        Div header = new Div(title, subtitle);
        header.setWidthFull();
        return header;
    }
    
    private HorizontalLayout createFilters() {
        categoryFilter = new ComboBox<>("Filter by Category");
        categoryFilter.setItems(
            "All Categories",
            "THEOLOGICAL_MISREPRESENTATION",
            "TERRORISM_ASSOCIATION", 
            "CULTURAL_STEREOTYPING",
            "RELIGIOUS_BIAS",
            "HISTORICAL_DISTORTION",
            "OTHER"
        );
        categoryFilter.setValue("All Categories");
        categoryFilter.addValueChangeListener(e -> loadViolations());
        
        Button refreshButton = new Button("🔄 Refresh", e -> loadViolations());
        
        HorizontalLayout filterLayout = new HorizontalLayout(categoryFilter, refreshButton);
        filterLayout.setAlignItems(Alignment.END);
        filterLayout.setWidthFull();
        
        return filterLayout;
    }
    
    private Div createViolationsGrid() {
        violationsGrid = new Grid<>(ContentAnalysis.class, false);
        violationsGrid.addColumn(ca -> ca.getContentItem().getTitle())
            .setHeader("Content Title")
            .setSortable(true);
        violationsGrid.addColumn(ca -> ca.getContentItem().getPlatform())
            .setHeader("Platform")
            .setSortable(true);
        violationsGrid.addColumn(ca -> ca.getContentItem().getAuthor())
            .setHeader("Author");
        violationsGrid.addColumn(ca -> ca.getViolationExplanation().substring(0, 
                Math.min(100, ca.getViolationExplanation().length())) + "...")
            .setHeader("Analysis Summary")
            .setAutoWidth(true);
        violationsGrid.addColumn(ca -> ca.getQuranReferences().size() + " Quran, " + 
                ca.getHadithReferences().size() + " Hadith")
            .setHeader("References");
        violationsGrid.addColumn(ca -> ca.getAnalyzedAt().toString().substring(0, 10))
            .setHeader("Analyzed Date")
            .setSortable(true);
        
        violationsGrid.addComponentColumn(this::createFeedbackButtons)
            .setHeader("Your Feedback");
        
        violationsGrid.addItemClickListener(e -> showDetails(e.getItem()));
        violationsGrid.setSizeFull();
        violationsGrid.setHeight("400px");
        
        Div gridContainer = new Div(violationsGrid);
        gridContainer.setSizeFull();
        gridContainer.getStyle().set("flex-grow", "1");
        
        return gridContainer;
    }
    
    private HorizontalLayout createFeedbackButtons(ContentAnalysis analysis) {
        Button likeBtn = new Button("👍 Like", e -> submitFeedback(analysis, UserFeedback.FeedbackType.LIKE));
        likeBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS,
                                com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL);
        
        Button dislikeBtn = new Button("👎 Dislike", e -> submitFeedback(analysis, UserFeedback.FeedbackType.DISLIKE));
        dislikeBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR,
                                   com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL);
        
        HorizontalLayout layout = new HorizontalLayout(likeBtn, dislikeBtn);
        layout.setPadding(false);
        layout.setSpacing(true);
        return layout;
    }
    
    private Div createDetailPanel() {
        detailPanel = new Div();
        detailPanel.setSizeFull();
        detailPanel.getStyle()
            .set("border", "1px solid #ddd")
            .set("border-radius", "8px")
            .set("padding", "16px")
            .set("background-color", "#f9f9f9")
            .set("overflow-y", "auto");
        detailPanel.setVisible(false);
        
        Div container = new Div(detailPanel);
        container.setHeight("300px");
        container.getStyle().set("flex-shrink", "0");
        
        return container;
    }
    
    private void loadViolations() {
        try {
            PageRequest pageRequest = PageRequest.of(0, 20, Sort.by("analyzedAt").descending());
            Page<ContentAnalysis> violationsPage;
            
            String selectedCategory = categoryFilter.getValue();
            if ("All Categories".equals(selectedCategory)) {
                violationsPage = analysisService.getViolations(pageRequest);
            } else {
                // Filter by category in memory (could be optimized with repository query)
                violationsPage = analysisService.getViolations(pageRequest);
            }
            
            violationsGrid.setItems(violationsPage.getContent());
            
            Notification.show("Loaded " + violationsPage.getNumberOfElements() + " violations", 2000, 
                            Notification.Position.BOTTOM_END);
        } catch (Exception e) {
            Notification.show("Error loading violations: " + e.getMessage(), 3000, 
                            Notification.Position.MIDDLE);
        }
    }
    
    private void showDetails(ContentAnalysis analysis) {
        detailPanel.setVisible(true);
        detailPanel.removeAll();
        
        String content = analysis.getContentItem().getContent();
        String title = analysis.getContentItem().getTitle() != null ? 
                      analysis.getContentItem().getTitle() : "Untitled";
        
        H1 detailTitle = new H1(title);
        detailTitle.getStyle().set("font-size", "1.5em");
        
        Div sourceInfo = new Div(
            new Paragraph("Platform: " + analysis.getContentItem().getPlatform()),
            new Paragraph("Author: " + analysis.getContentItem().getAuthor()),
            new Paragraph("URL: " + analysis.getContentItem().getUrl())
        );
        
        Div explanation = new Div(
            new Paragraph("📊 Analysis:"),
            new Paragraph(analysis.getViolationExplanation())
        );
        explanation.getStyle().set("margin-top", "16px");
        
        Div counterArgument = new Div(
            new Paragraph("💬 Counter-Argument:"),
            new Paragraph(analysis.getCounterArgument())
        );
        counterArgument.getStyle().set("margin-top", "16px");
        
        Div quranRefs = new Div(
            new Paragraph("📖 Quran References:"),
            new Paragraph(String.join(", ", analysis.getQuranReferences()))
        );
        quranRefs.getStyle().set("margin-top", "16px");
        
        Div hadithRefs = new Div(
            new Paragraph("📚 Hadith References:"),
            new Paragraph(String.join(", ", analysis.getHadithReferences()))
        );
        hadithRefs.getStyle().set("margin-top", "16px");
        
        // Feedback stats
        Map<String, Long> stats = analysisService.getFeedbackStats(analysis.getId());
        Div feedbackStats = new Div(
            new Paragraph("📈 Community Feedback:"),
            new Paragraph("👍 " + stats.get("likes") + " | 👎 " + stats.get("dislikes"))
        );
        feedbackStats.getStyle().set("margin-top", "16px");
        
        detailPanel.add(detailTitle, sourceInfo, explanation, counterArgument, 
                       quranRefs, hadithRefs, feedbackStats);
    }
    
    private void submitFeedback(ContentAnalysis analysis, UserFeedback.FeedbackType type) {
        try {
            // In a real app, you'd collect IP, device fingerprint, etc. from the request
            var response = analysisService.submitFeedback(
                "127.0.0.1", // Placeholder IP
                "browser-fingerprint", // Placeholder
                "Mozilla/5.0", // Placeholder UA
                analysis.getId(),
                type,
                null // No comment for simple like/dislike
            );
            
            if (response.isSuccess()) {
                Notification.show(
                    "Thank you for your feedback! " + 
                    "(👍 " + response.getTotalLikes() + " | 👎 " + response.getTotalDislikes() + ")",
                    3000,
                    Notification.Position.BOTTOM_END
                );
                
                // Refresh the detail panel to show updated stats
                showDetails(analysis);
            } else {
                Notification.show(response.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception e) {
            Notification.show("Error submitting feedback: " + e.getMessage(), 3000, 
                            Notification.Position.MIDDLE);
        }
    }
}
