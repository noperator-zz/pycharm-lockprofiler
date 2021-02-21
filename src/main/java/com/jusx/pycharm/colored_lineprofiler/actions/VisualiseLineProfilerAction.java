package com.jusx.pycharm.colored_lineprofiler.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.jusx.pycharm.colored_lineprofiler.service.ProfileHighlightService;
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
        if (profileFile == null || profileFile.getExtension() == null || !profileFile.getExtension().equals("lprof")) {
            logger.error("Can not load profile file (" + profileFile + ")");
            return;
        }

        Project currentProject = event.getProject();
        assert currentProject != null;

        SettingsState settings = SettingsState.getInstance();
        Sdk lprofConversionSdk = settings.getSdk(currentProject);

        if (lprofConversionSdk == null) {
            // TODO send notification?
            logger.error("Can not convert .lprof file to a readable format: no project interpreter is set for project (" + currentProject.getName() + ")");
            return;
        }


        ProfileHighlightService profileHighlightService = currentProject.getService(ProfileHighlightService.class);

        profileHighlightService.loadLineProfile(lprofConversionSdk, profileFile);
    }
}
