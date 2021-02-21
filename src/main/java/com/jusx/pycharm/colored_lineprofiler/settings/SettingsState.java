package com.jusx.pycharm.colored_lineprofiler.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.python.sdk.PythonSdkType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 *
 * Based on:
 * https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html#the-appsettingsstate-class
 */
@State(
        name = "com.jusx.pycharm.colored_lineprofiler.settings.SettingsState",
        storages = {@Storage("ColoredLineprofilerPlugin.xml")}
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

//    public String userId = "John Q. Public";
    public String sdkHomePath = null;

    public static SettingsState getInstance() {
        return ServiceManager.getService(SettingsState.class);
    }

    @Nullable
    @Override
    public SettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public void setSdk(@Nullable Sdk sdk) {
        if (sdk == null) {
            this.sdkHomePath = null;
        } else {
            this.sdkHomePath = sdk.getHomePath();
        }
    }

    @Nullable
    public Sdk getSdk(Project project) {
        if (this.sdkHomePath == null) {
            return ProjectRootManager.getInstance(project).getProjectSdk();
        }

        List<Sdk> sdks = ProjectJdkTable.getInstance().getSdksOfType(PythonSdkType.getInstance());

        for (Sdk checkSdk : sdks) {
            String sdkHomePath = checkSdk.getHomePath();
            if (sdkHomePath != null && sdkHomePath.equals(this.sdkHomePath)) {
                return checkSdk;
            }
        }
        return ProjectRootManager.getInstance(project).getProjectSdk();
    }
}