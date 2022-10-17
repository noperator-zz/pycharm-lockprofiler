package nl.jusx.pycharm.lockprofiler.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import nl.jusx.pycharm.lockprofiler.profile.Profile;
import nl.jusx.pycharm.lockprofiler.render.LockProfilerToolWindowFactory;
import nl.jusx.pycharm.lockprofiler.service.ProfileHighlightService;
import nl.jusx.pycharm.lockprofiler.service.TimeFractionCalculation;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE;

public class VisualiseLineProfilerAction extends AnAction {
    private static final Logger logger = Logger.getInstance(VisualiseLineProfilerAction.class.getName());

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
    }

    protected TimeFractionCalculation withTimeFractionCalculation() {
        return TimeFractionCalculation.FUNCTION_TOTAL;
    }
}
