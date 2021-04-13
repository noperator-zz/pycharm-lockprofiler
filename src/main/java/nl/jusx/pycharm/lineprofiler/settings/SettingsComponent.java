package nl.jusx.pycharm.lineprofiler.settings;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 *
 * Based on
 * https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html#the-appsettingscomponent-class
 */
public class SettingsComponent {
    private final JPanel myMainPanel;

//    private final ButtonGroup defaultTimeFractionCalculation = new ButtonGroup();
//    private final JBRadioButton functionTotalButton = new JBRadioButton("Total function times", true);
//    private final JBRadioButton profileTotalButton = new JBRadioButton("Total profile time");

    public SettingsComponent() {
//        defaultTimeFractionCalculation.add(profileTotalButton);
//        defaultTimeFractionCalculation.add(functionTotalButton);

        myMainPanel = FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("No settings exist for 'Line Profiler Plugin'"))
//                .addComponent(new JBLabel("As a default, visualizations should express percentages of"), 1)
//                .addComponent(functionTotalButton, 1)
//                .addComponent(profileTotalButton, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return myMainPanel;
    }

//    public JComponent getPreferredFocusedComponent() {
//        return profileTotalButton;
//    }
//
//    public void setDefaultTimeFractionCalculation(TimeFractionCalculation timeFractionCalculation) {
//        if (timeFractionCalculation == TimeFractionCalculation.PROFILE_TOTAL) {
//            defaultTimeFractionCalculation.setSelected(profileTotalButton.getModel(), true);
//        } else {
//            defaultTimeFractionCalculation.setSelected(functionTotalButton.getModel(), true);
//        }
//    }
//
//    public TimeFractionCalculation getDefaultTimeFractionCalculation() {
//        if (defaultTimeFractionCalculation.getSelection() == profileTotalButton.getModel()) {
//            return TimeFractionCalculation.PROFILE_TOTAL;
//        } else {
//            return TimeFractionCalculation.FUNCTION_TOTAL;
//        }
//    }
}