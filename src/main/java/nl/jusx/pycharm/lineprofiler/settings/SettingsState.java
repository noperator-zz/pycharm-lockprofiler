package nl.jusx.pycharm.lineprofiler.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import nl.jusx.pycharm.lineprofiler.service.ColorMapOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 *
 * Based on:
 * https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html#the-appsettingsstate-class
 */
@State(
        name = "nl.jusx.pycharm.lineprofiler.settings.SettingsState",
        storages = {@Storage("LineprofilerPlugin.xml")}
)
public class SettingsState implements PersistentStateComponent<SettingsState> {
    private int tableAlignmentMaxColumns = 120;
    private ColorMapOption colorMap = ColorMapOption.VIRIDIS;

    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    public ColorMapOption getColorMap() {
        return colorMap;
    }

    public void setColorMap(ColorMapOption colorMap) {
        this.colorMap = colorMap;
    }

    public int getTableAlignmentMaxColumns() {
        return tableAlignmentMaxColumns;
    }

    public void setTableAlignmentMaxColumns(int tableAlignmentMaxColumns) {
        this.tableAlignmentMaxColumns = tableAlignmentMaxColumns;
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
}