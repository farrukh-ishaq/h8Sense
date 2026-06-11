package com.islamophobia.detector.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.stereotype.Component;

/**
 * Enables the shared Vaadin theme for the application shell.
 */
@Component
@Theme("h8sense")
public class AppShellConfig implements AppShellConfigurator {
}

