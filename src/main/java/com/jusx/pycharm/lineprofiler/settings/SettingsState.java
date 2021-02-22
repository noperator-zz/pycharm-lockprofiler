package com.jusx.pycharm.lineprofiler.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jusx.pycharm.lineprofiler.service.TimeFractionCalculation;
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
        name = "com.jusx.pycharm.lineprofiler.settings.SettingsState",
        storages = {@Storage("LineprofilerPlugin.xml")}
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

//    public String userId = "John Q. Public";
    public TimeFractionCalculation defaultTimeFractionCalculation = TimeFractionCalculation.FUNCTION_TOTAL;

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
}