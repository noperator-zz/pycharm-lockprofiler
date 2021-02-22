package com.jusx.pycharm.colored_lineprofiler.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.jusx.pycharm.colored_lineprofiler.profile.Profile;
import com.jusx.pycharm.colored_lineprofiler.service.ProfileHighlightService;
import com.jusx.pycharm.colored_lineprofiler.service.TimeFractionCalculation;
import com.jusx.pycharm.colored_lineprofiler.settings.SettingsState;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE;

public class VisualiseLineProfilerAction extends AnAction {
    private static final Logger logger = Logger.getInstance(VisualiseLineProfilerAction.class.getName());

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        VirtualFile profileFile = event.getData(VIRTUAL_FILE);
        if (profileFile == null || profileFile.getExtension() == null || !profileFile.getExtension().equals("pclprof")) {
            logger.error("Can not load profile file (" + profileFile + ")");
            return;
        }

        Project currentProject = event.getProject();
        assert currentProject != null;

        ProfileHighlightService profileHighlightService = currentProject.getService(ProfileHighlightService.class);

        Profile profile = Profile.fromPclprof(profileFile);

        profileHighlightService.setProfile(profile);
        profileHighlightService.visualizeProfile(withTimeFractionCalculation());
    }

    protected TimeFractionCalculation withTimeFractionCalculation() {
        return TimeFractionCalculation.PROFILE_TOTAL;
    }
}
