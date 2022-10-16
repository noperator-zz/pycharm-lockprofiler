package nl.jusx.pycharm.lockprofiler.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE;

public class VisualiseProfilerActionGroup extends DefaultActionGroup {
    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(VIRTUAL_FILE);
        if (file == null || file.getExtension() == null) {
            e.getPresentation().setVisible(false);
        } else e.getPresentation().setVisible(file.getExtension().equals("pclprof"));
        super.update(e);
    }
}
