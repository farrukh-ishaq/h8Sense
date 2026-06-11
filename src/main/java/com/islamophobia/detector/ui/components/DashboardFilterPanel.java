package com.islamophobia.detector.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class DashboardFilterPanel extends Div {

    private final ComboBox<String> statusFilter = new ComboBox<>("Analysis Status");
    private final ComboBox<String> categoryFilter = new ComboBox<>("Category");
    private final ComboBox<String> platformFilter = new ComboBox<>("Platform");
    private final Paragraph statusMessage = new Paragraph();

    public DashboardFilterPanel(Runnable onRefresh, Runnable onClearSelection, Runnable onFilterChange) {
        addClassNames("dashboard-card", "filter-card");

        statusFilter.setWidth("220px");
        categoryFilter.setWidth("220px");
        platformFilter.setWidth("220px");

        statusFilter.addValueChangeListener(event -> onFilterChange.run());
        categoryFilter.addValueChangeListener(event -> onFilterChange.run());
        platformFilter.addValueChangeListener(event -> onFilterChange.run());

        Button refreshButton = new Button("⟳ Refresh", event -> onRefresh.run());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button clearSelectionButton = new Button("Clear Selection", event -> onClearSelection.run());
        clearSelectionButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        statusMessage.addClassName("status-message");

        HorizontalLayout row = new HorizontalLayout(
            statusFilter,
            categoryFilter,
            platformFilter,
            refreshButton,
            clearSelectionButton,
            statusMessage
        );
        row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        row.addClassName("filter-row");
        row.setWidthFull();

        add(row);
    }

    public ComboBox<String> getStatusFilter() {
        return statusFilter;
    }

    public ComboBox<String> getCategoryFilter() {
        return categoryFilter;
    }

    public ComboBox<String> getPlatformFilter() {
        return platformFilter;
    }

    public void setStatusMessage(String message) {
        statusMessage.setText(message);
    }
}

