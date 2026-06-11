package com.islamophobia.detector.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;

public class DashboardFooter extends Div {

    public DashboardFooter() {
        addClassName("app-footer");

        Div footerCard = new Div();
        footerCard.addClassName("dashboard-card");

        H3 title = new H3("Operator Notes");
        title.addClassName("section-title");

        Paragraph p1 = new Paragraph(
            "This layout now follows a clearer shell structure: header and footer stay stable, while the body is composed of summary cards, filters, a results grid, and a detail inspector."
        );
        Paragraph p2 = new Paragraph(
            "Most previous inline styling has been moved into the shared Vaadin theme so future UI work can follow reusable classes instead of ad hoc per-component styling."
        );

        footerCard.add(title, p1, p2);
        add(footerCard);
    }
}

