package nl.jusx.pycharm.lineprofiler.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Provides controller functionality for application settings.
 */
public class SettingsConfigurable implements Configurable {
    private final Project project;
    private SettingsComponent mySettingsComponent;

    SettingsConfigurable(Project project) {
        this.project = project;
    }

    // A default constructor with no arguments is required because this implementation
    // is registered as an applicationConfigurable EP

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Line Profiler";
    }

//    @Override
//    public JComponent getPreferredFocusedComponent() {
//        return mySettingsComponent.getPreferredFocusedComponent();
//    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mySettingsComponent = new SettingsComponent();
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
//        SettingsState settings = SettingsState.getInstance();
//
//        boolean modified = mySettingsComponent.getDefaultTimeFractionCalculation() != settings.defaultTimeFractionCalculation;
//
//        return modified;
        return false;
    }

    @Override
    public void apply() {
//        SettingsState settings = SettingsState.getInstance();
//        settings.defaultTimeFractionCalculation = mySettingsComponent.getDefaultTimeFractionCalculation();
    }

    @Override
    public void reset() {
//        SettingsState settings = SettingsState.getInstance();
//        mySettingsComponent.setDefaultTimeFractionCalculation(settings.defaultTimeFractionCalculation);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }

}