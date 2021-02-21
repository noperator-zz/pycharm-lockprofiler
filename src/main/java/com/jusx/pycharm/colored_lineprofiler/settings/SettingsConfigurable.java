package com.jusx.pycharm.colored_lineprofiler.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.python.sdk.PythonSdkType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

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
        return "Colored Lineprofiler";
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

        boolean modified = mySettingsComponent.getSelectedLprofConversionSdk() != settings.getSdk(project);

        return modified;
    }

    @Override
    public void apply() {
        SettingsState settings = SettingsState.getInstance();
        settings.setSdk(mySettingsComponent.getSelectedLprofConversionSdk());
    }

    @Override
    public void reset() {
        SettingsState settings = SettingsState.getInstance();

        List<Sdk> sdks = ProjectJdkTable.getInstance().getSdksOfType(PythonSdkType.getInstance());
        mySettingsComponent.setSdkList(sdks);
        mySettingsComponent.setSelectedLprofConversionSdk(settings.getSdk(project));
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }

}