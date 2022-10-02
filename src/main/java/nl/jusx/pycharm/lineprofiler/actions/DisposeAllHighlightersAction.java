package nl.jusx.pycharm.lineprofiler.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import nl.jusx.pycharm.lineprofiler.service.ProfileHighlightService;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE;

public class DisposeAllHighlightersAction extends AnAction {
    private static final Logger logger = Logger.getInstance(VisualiseLineProfilerAction.class.getName());

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project currentProject = e.getProject();
        assert currentProject != null;
        ProfileHighlightService profileHighlightService = currentProject.getService(ProfileHighlightService.class);
        VirtualFile srcFile = e.getData(VIRTUAL_FILE);
        // Only enable this action when there is something to remove
        e.getPresentation().setEnabled(profileHighlightService.containsVisualizations(srcFile));
        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project currentProject = event.getProject();
        assert currentProject != null;

        ProfileHighlightService profileHighlightService = currentProject.getService(ProfileHighlightService.class);
        VirtualFile srcFile = event.getData(VIRTUAL_FILE);
        if (srcFile == null) {
            logger.error("Can not dispose line-profiler visualisation for file because it is not found: " + srcFile);
            return;
        }
        logger.info("Disposing all highlighters from " + srcFile);
        profileHighlightService.disposeVisualizations(srcFile);
    }
}
