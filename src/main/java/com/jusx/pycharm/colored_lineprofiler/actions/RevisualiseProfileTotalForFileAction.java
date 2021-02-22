package com.jusx.pycharm.colored_lineprofiler.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jusx.pycharm.colored_lineprofiler.service.ProfileHighlightService;
import com.jusx.pycharm.colored_lineprofiler.service.TimeFractionCalculation;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE;

public class RevisualiseProfileTotalForFileAction extends AnAction {
    private static final Logger logger = Logger.getInstance(VisualiseLineProfilerAction.class.getName());

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project currentProject = e.getProject();
        assert currentProject != null;
        ProfileHighlightService profileHighlightService = currentProject.getService(ProfileHighlightService.class);
        VirtualFile srcFile = e.getData(VIRTUAL_FILE);
        // Only enable this action when there is something to revisualize
        e.getPresentation().setEnabled(profileHighlightService.containsHighlights(srcFile)
                && profileHighlightService.currentTimeFractionCalculation(srcFile) != withTimeFractionCalculation());

        if (profileHighlightService.containsHighlights(srcFile) && profileHighlightService.currentTimeFractionCalculation(srcFile) == withTimeFractionCalculation()) {
            // Set check icon if action is already active
            e.getPresentation().setIcon(AllIcons.Actions.Checked);
        } else {
            // Remove check icon if action can be clicked
            e.getPresentation().setIcon(null);
        }

        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project currentProject = event.getProject();
        assert currentProject != null;

        ProfileHighlightService profileHighlightService = currentProject.getService(ProfileHighlightService.class);

        VirtualFile srcFile = event.getData(VIRTUAL_FILE);
        if (srcFile == null) {
            logger.error("Can not visualise lineprofile for file because it is not found: " + srcFile);
            return;
        }

        profileHighlightService.visualizeProfile(withTimeFractionCalculation(), srcFile);
    }

    protected TimeFractionCalculation withTimeFractionCalculation() {
        return TimeFractionCalculation.PROFILE_TOTAL;
    }
}
