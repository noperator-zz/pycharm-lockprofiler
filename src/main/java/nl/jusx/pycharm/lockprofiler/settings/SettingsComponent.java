package nl.jusx.pycharm.lockprofiler.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import nl.jusx.pycharm.lockprofiler.service.ColorMapOption;

import javax.swing.*;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 *
 * Based on
 * https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html#the-appsettingscomponent-class
 */
public class SettingsComponent {
    private final JPanel myMainPanel;

    private final JBIntSpinner myMaxTableAlignment = new JBIntSpinner(120, 0, 300);
    private final ComboBox<ColorMapOption> myColorMap = new ComboBox<>(ColorMapOption.values());


    public SettingsComponent() {
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Colormap: "), myColorMap, 1, false)
                .addLabeledComponent(new JBLabel("Max table alignment at column: "), myMaxTableAlignment, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return myMainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return myColorMap;
    }

    public void setColorMap(ColorMapOption colorMap) {
        myColorMap.setItem(colorMap);
    }

    public ColorMapOption getColorMap() {
        return myColorMap.getItem();
    }

    public void setMaxTableAlignment(int maxTableAlignment) {
        myMaxTableAlignment.setValue(maxTableAlignment);
    }

    public int getMaxTableAlignment() {
        return (int) myMaxTableAlignment.getValue();
    }
}
