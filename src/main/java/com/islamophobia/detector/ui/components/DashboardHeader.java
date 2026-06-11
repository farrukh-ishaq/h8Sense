package com.islamophobia.detector.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class DashboardHeader extends Div {

    public DashboardHeader() {
        addClassName("app-header");

        H1 title = new H1("🛡️ h8Sense Analysis Console");
        title.addClassName("app-header__title");

        Paragraph subtitle = new Paragraph(
            "A richer operator dashboard for exploring analyses, source quality, evidence references, model metadata, and feedback."
        );
        subtitle.addClassName("app-header__subtitle");

        HorizontalLayout pillRow = new HorizontalLayout(
            DashboardViewSupport.createBadge("Header stays stable", "badge--info"),
            DashboardViewSupport.createBadge("Body is component-driven", "badge--neutral"),
            DashboardViewSupport.createBadge("Footer holds operator notes", "badge--success")
        );
        pillRow.addClassName("app-header__pills");
        pillRow.setSpacing(true);

        add(title, subtitle, pillRow);
    }
}

