package com.jusx.pycharm.lineprofiler.utils;


import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;



public class LineprofilerInstallDialogWrapper extends DialogWrapper  {
    private final Sdk sdk;
    private final String explanation;

    LineprofilerInstallDialogWrapper(Sdk sdk, String explanation) {
        super(true); // use current window as parent
        this.sdk = sdk;
        this.explanation = explanation;
        init();
        setTitle("Install Line-Profiler");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {

        JPanel dialogPanel = new JPanel(new BorderLayout());

        JLabel label = new JLabel(explanation);
        label.setPreferredSize(new Dimension(100, 30));
        dialogPanel.add(label, BorderLayout.NORTH);

        JLabel label2 = new JLabel("Do you want to install line-profiler-pycharm to " + sdk.getName() + "?");
        label.setPreferredSize(new Dimension(100, 30));
        dialogPanel.add(label2, BorderLayout.SOUTH);

        return dialogPanel;
    }
}