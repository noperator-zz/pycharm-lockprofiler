package com.jusx.pycharm.colored_lineprofiler.profile;


import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.python.run.PythonProcessRunner;
import com.jusx.pycharm.colored_lineprofiler.utils.LineProfilerSdkUtils;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Profile {
    private static final Logger logger = Logger.getInstance(Profile.class.getName());

    List<FunctionProfile> functionProfiles = new ArrayList<>();
    private final float unit;
    private float totalTime;

    private Profile(ProfileSchema schema) {
        unit = schema.unit;

        for (ProfileSchema.Function fSchema : schema.profiledFunctions) {
            FunctionProfile fn = new FunctionProfile(fSchema);
            functionProfiles.add(fn);
            totalTime += fn.totalTime;
        }
    }

    public List<FunctionProfile> getProfiledFunctions() {
        return functionProfiles;
    }

    public float getUnit() {
        return unit;
    }

    public float getTotalTime() {
        return totalTime;
    }


    /**
     * Loads an .lprof file into a Profile object
     * @param project project to use
     * @param conversionSdk python environment to use that can convert the profile object
     *                      this environment must have line-profiler installed.
     * @param profileFile .lprof file to load
     * @return Profile object from .lprof file
     */
    @Nullable
    public static Profile fromLprof(Project project,
                             Sdk conversionSdk,
                             VirtualFile profileFile) {
        // Check whether lineprofiler is installed
        boolean lineProfilerInstalled;
        try {
            lineProfilerInstalled = LineProfilerSdkUtils.hasLineProfilerInstalled(conversionSdk);
        } catch (ExecutionException e) {
            logger.error("Could not check whether line-profiler was installed to sdk " + conversionSdk, e);
            return null;
        }
        if (!lineProfilerInstalled) {
            lineProfilerInstalled = LineProfilerSdkUtils.requestAndDoInstallation(
                    project,
                    conversionSdk,
                    "Package line-profiler is not installed to python environment."
            );
            if (!lineProfilerInstalled) {
                logger.warn("Package installation line-profiler to sdk " + conversionSdk + " did not succeed");
                return null;
            }
        }

        // Create temporary file to which readable profile json will be written.
        Path tempProfileJson;
        try {
            tempProfileJson = Files.createTempFile(profileFile.getName() + "-converted", ".json");
        } catch (IOException e) {
            logger.error("Could not create temporary file for converted .lprof file", e);
            return null;
        }

        // Convert lprof to json
        boolean profileFileConversion = convertLprofToJson(
                project,
                conversionSdk,
                profileFile,
                tempProfileJson
        );

        // Check success of profilefile json conversion and follow up depending on outcome.
        if (profileFileConversion) {
            // Conversion of .lprof file successful, visualize the profile!
            return fromJson(tempProfileJson.toString());
        }
        return null;
    }

    /**
     * Creates profile object from json
     *
     * Json should be generated with the `load_kernprof_file.json` file that comes with
     * this package
     *
     * @param fileName Path to json
     * @return Profile
     */
    private static Profile fromJson(String fileName) {
        Gson gson = new Gson();
        JsonReader reader;

        try {
            reader = new JsonReader(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        ProfileSchema data = gson.fromJson(reader, ProfileSchema.class); // contains the whole reviews list

        return new Profile(data);
    }

    /**
     * Converts a .lprof file to a readable .json
     *
     * Also sends notifications to IDE when failures require user action.
     * Conversion is done with the `load_kernprof_file.py`
     *
     * @param project PyCharm project that runs conversion script
     * @param conversionSdk python sdk to convert lprof with
     *                      line-profiler must be installed to this sdk
     * @param profileFile lprof file
     * @param tempProfileJson json output file
     * @return boolean indicating whether conversion was successful
     */
    private static boolean convertLprofToJson(Project project, Sdk conversionSdk, VirtualFile profileFile, Path tempProfileJson) {
        // Get interpreter path from sdk
        String interpreterPath = conversionSdk.getHomePath();
        if (interpreterPath == null) {
            // TODO send notification?
            logger.error("Python env " + conversionSdk + " has no interpreter set up");
            return false;
        }

        // Read the profile conversion script so that it can be run as python `-c` argument
        InputStream convertProfileScriptStream = Profile.class.getClassLoader().getResourceAsStream("load_kernprof_file.py");
        assert convertProfileScriptStream != null;
        String convertProfileScript;
        try {
            convertProfileScript = new String(convertProfileScriptStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Could not read profile conversion python-script", e);
            return false;
        }

        // Setup the command line that will execute the conversion
        GeneralCommandLine gcl = new GeneralCommandLine()
                .withWorkDirectory(tempProfileJson.getParent().toString())
                .withExePath(interpreterPath)
                .withParameters(
                        "-c",
                        convertProfileScript,
                        profileFile.getPath(),
                        tempProfileJson.toString());

        // Execute conversion
        ProcessHandler processHandler;
        try {
            processHandler = PythonProcessRunner.createProcess(gcl);
        } catch (ExecutionException e) {
            logger.error("Converting " + profileFile + " failed: could not start process", e);
            return false;
        }
        // Wait for conversion completion
        ProgressManagerImpl.getInstance().runProcessWithProgressSynchronously(
                () -> {
                    processHandler.startNotify();
                    processHandler.waitFor();
                    logger.warn("Exit code of profile conversion: " + processHandler.getExitCode());
                },
                "Converting line profile to json", false, project
        );
        Integer convertProfileFileExitCode = processHandler.getExitCode();
        assert convertProfileFileExitCode != null;

        // Check success of profilefile json conversion and follow up depending on outcome.
        if (convertProfileFileExitCode == 0) {
            // Conversion of .lprof file successful
            return true;
        } else if (convertProfileFileExitCode == 2) {
            // Pickle protocol error
            NotificationGroupManager.getInstance().getNotificationGroup("Colored Lineprofiler Notifications")
                    .createNotification(
                            "Could not read " + profileFile.getName() + " because it was created with a conflicting " +
                                    "version of pickle. Consider changing the Python Environment for " +
                                    "loading .lprof files in settings (Tools -> Colored Lineprofiler)",
                            NotificationType.ERROR)
                    .notify(project);
        } else {
            NotificationGroupManager.getInstance().getNotificationGroup("Colored Lineprofiler Notifications")
                    .createNotification(
                            "Could not read " + profileFile.getName() + " because of an unknown error",
                            NotificationType.ERROR)
                    .notify(project);
        }
        return false;
    }
}
