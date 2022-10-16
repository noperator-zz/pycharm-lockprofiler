package nl.jusx.pycharm.lockprofiler.executor;

import com.intellij.execution.Executor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.wm.ToolWindowId;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ProfileExecutor extends Executor {
    @NonNls public static final String PROFILE_EXECUTOR_ID = "PROFILE_LINES";

    @Override
    public @NotNull String getToolWindowId() {
        return ToolWindowId.RUN;
    }

    @Override
    public @NotNull Icon getToolWindowIcon() {
        return AllIcons.Toolwindows.ToolWindowRun;
    }

    @Override
    public @NotNull Icon getIcon() {
        return AllIcons.Actions.Profile;
    }

    @Override
    public Icon getDisabledIcon() {
        return IconLoader.getDisabledIcon(getIcon());
    }

    @Override
    public @NlsActions.ActionDescription String getDescription() {
        return "Profile Lines in selected configuration";
    }

    @Override
    public @NotNull @NlsActions.ActionText String getActionName() {
        return "Profile Lines";
    }

    @Override
    public @NotNull @NonNls String getId() {
        return PROFILE_EXECUTOR_ID;
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getStartActionText() {
        return "P&rofile Lines";
    }

    @Override
    public @NonNls String getContextActionId() {
        return "LineProfileClass";
    }

    @Override
    public @NonNls String getHelpId() {
        return "ideaInterface.profileLines";
    }
}
