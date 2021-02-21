package com.jusx.pycharm.colored_lineprofiler.settings;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jdesktop.swingx.combobox.ListComboBoxModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 *
 * Based on
 * https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html#the-appsettingscomponent-class
 */
public class SettingsComponent {
    private final JPanel myMainPanel;

    private final List<Sdk> sdkList = new ArrayList<>();
    private final ListComboBoxModel<Sdk> comboBoxModel = new ListComboBoxModel<>(sdkList);
    private final ComboBox<Sdk> defaultLprofConversionSdk = new ComboBox<Sdk>(comboBoxModel);

    public SettingsComponent() {
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Default python env for converting .lprof files: "),
                        defaultLprofConversionSdk, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return myMainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return defaultLprofConversionSdk;
    }

    public void setSdkList(List<Sdk> sdkList) {
        this.sdkList.clear();
        this.sdkList.addAll(sdkList);
    }

    public void setSelectedLprofConversionSdk(Sdk sdk) {
        this.comboBoxModel.setSelectedItem(sdk);
    }

    @Nullable
    public Sdk getSelectedLprofConversionSdk() {
        return this.comboBoxModel.getSelectedItem();
    }
}