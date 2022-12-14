package nl.jusx.pycharm.lockprofiler.settings;

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

    @Override
    public JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mySettingsComponent = new SettingsComponent();
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        SettingsState settings = SettingsState.getInstance();

        return mySettingsComponent.getColorMap() != settings.getColorMap() ||
                mySettingsComponent.getMaxTableAlignment() != settings.getTableAlignmentMaxColumns();
    }

    @Override
    public void apply() {
        SettingsState settings = SettingsState.getInstance();
        settings.setColorMap(mySettingsComponent.getColorMap());
        settings.setTableAlignmentMaxColumns(mySettingsComponent.getMaxTableAlignment());
    }

    @Override
    public void reset() {
        SettingsState settings = SettingsState.getInstance();
        mySettingsComponent.setColorMap(settings.getColorMap());
        mySettingsComponent.setMaxTableAlignment(settings.getTableAlignmentMaxColumns());
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }

}
