package com.jusx.pycharm.lineprofiler.executor;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import com.jetbrains.python.run.PythonRunConfiguration;
import com.jetbrains.python.run.PythonRunner;
import com.jusx.pycharm.lineprofiler.profile.Profile;
import com.jusx.pycharm.lineprofiler.service.ProfileHighlightService;
import com.jusx.pycharm.lineprofiler.settings.SettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class ProfileRunner extends PythonRunner {
    private static final Logger logger = Logger.getInstance(ProfileRunner.class.getName());

    @NotNull
    @Override
    public String getRunnerId() {
        return ProfileExecutor.PROFILE_EXECUTOR_ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(ProfileExecutor.PROFILE_EXECUTOR_ID) && profile instanceof AbstractPythonRunConfiguration;
    }

    /**
     * Starts the Profile Lines execution
     *
     * If a .pclprof file is written during execution it is visualized after completion
     *
     * Location of the .pclprof file is defined with the `line-profiler-pycharm` environment
     * variable:
     *  PC_LINE_PROFILER_STATS_FILENAME
     */
    @Override
    protected @NotNull Promise<@Nullable RunContentDescriptor> execute(@NotNull ExecutionEnvironment env, @NotNull RunProfileState state) {
        ProfileHighlightService profileHighlightService = env.getProject().getService(ProfileHighlightService.class);
        ApplicationManager.getApplication().invokeLater(profileHighlightService::disposeAllHighlighters);

        PythonRunConfiguration runConfiguration = (PythonRunConfiguration) env.getRunProfile();

        // Define the location where the .pclprof file must be saved by our `line-profiler-pycharm` python package
        Path pclprofPath;
        String pclprofFilename;
        if (runConfiguration.getWorkingDirectory() == null || runConfiguration.getWorkingDirectory().equals("")) {
            // Create temporary file to which a .pclprof will be written.
            try {
                pclprofPath = Files.createTempFile("lineprofile", ".pclprof");
                // Use absolute path as file name for temporary file
                pclprofFilename = pclprofPath.toString();
                pclprofFilename = FileUtil.getNameWithoutExtension(pclprofFilename);
            } catch (IOException e) {
                throw new RuntimeException("Could not create temporary .pclprof file for Profile Line execution");
            }
        } else {
            // Set filename of script as filename path
            // a module script `some_module` would become `some_module`
            // a normal script `some_dir/some_script.py` would become `some_script.py`
            // .pclprof will be written to the working directory
            pclprofFilename = Paths.get(runConfiguration.getScriptName()).getFileName().toString();
            pclprofPath = Paths.get(runConfiguration.getWorkingDirectory(), pclprofFilename + ".pclprof");
        }

        // Remove file if it already exists
        try {
            Files.deleteIfExists(pclprofPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not remove already existing " + pclprofPath + " for Profile Line execution");
        }

        // Set the environment variable for `line-profile-pycharm`
        runConfiguration.getEnvs().put("PC_LINE_PROFILER_STATS_FILENAME", pclprofFilename);

        // Truly start the python execution
        Promise<RunContentDescriptor> promise = super.execute(env, state);

        // After creating an execution, the environment variable can be removed again from our run config
        promise.then(runContentDescriptor -> {
            runConfiguration.getEnvs().remove("PC_LINE_PROFILER_STATS_FILENAME");
            return runContentDescriptor;
        });

        // Add listener to process completion that triggers profile visualization
        promise.onSuccess(runContentDescriptor -> {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                ProcessHandler ph = runContentDescriptor.getProcessHandler();
                if (ph == null) {
                    logger.error("Could not get processhandler after starting Line Profile run");
                    return;
                }
                if (ph.waitFor()) {
                    triggerPclprofVisualization(env.getProject(), pclprofPath);
                }
            });
        });

        return promise;
    }

    /**
     * Looks for the just created .pclprof file and visualizes it
     *
     * The .pclprof file should have been created by `line-profiler-pycharm` python package
     *
     * @param project project
     * @param pclprofPath path to .pclprof file
     */
    private void triggerPclprofVisualization(Project project, Path pclprofPath) {
        if (!Files.exists(pclprofPath)) {
            // No .pclprof file has been created during profile, visualization not possible
            return;
        }

        ProfileHighlightService profileHighlightService = project.getService(ProfileHighlightService.class);

        Profile profile = Profile.fromPclprof(pclprofPath.toString());
        SettingsState settings = SettingsState.getInstance();

        ApplicationManager.getApplication().invokeLater(() -> {
            profileHighlightService.setProfile(profile);
            profileHighlightService.visualizeProfile(settings.defaultTimeFractionCalculation);
        });
    }
}
