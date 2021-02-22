package com.jusx.pycharm.colored_lineprofiler.executor;

public class ExecutorWIP {
//    /**
//     * Loads an .lprof file into a Profile object
//     * @param project project to use
//     * @param conversionSdk python environment to use that can convert the profile object
//     *                      this environment must have line-profiler-pycharm installed.
//     * @param profileFile .lprof file to load
//     * @return Profile object from .lprof file
//     */
//    @Nullable
//    public static Profile fromLprof(Project project,
//                                    Sdk conversionSdk,
//                                    VirtualFile profileFile) {
//        // Check whether lineprofiler is installed
//        boolean lineProfilerInstalled;
//        try {
//            lineProfilerInstalled = LineProfilerPycharmSdkUtils.hasLineProfilerInstalled(conversionSdk);
//        } catch (ExecutionException e) {
//            logger.error("Could not check whether line-profiler-pycharm was installed to sdk " + conversionSdk, e);
//            return null;
//        }
//        if (!lineProfilerInstalled) {
//            lineProfilerInstalled = LineProfilerPycharmSdkUtils.requestAndDoInstallation(
//                    project,
//                    conversionSdk,
//                    "Package line-profiler-pycharm is not installed to python environment."
//            );
//            if (!lineProfilerInstalled) {
//                logger.warn("Package installation line-profiler-pycharm to sdk " + conversionSdk + " did not succeed");
//                return null;
//            }
//        }
//
//        // Create temporary file to which readable profile json will be written.
//        Path tempProfileJson;
//        try {
//            tempProfileJson = Files.createTempFile(profileFile.getName() + "-converted", ".json");
//        } catch (IOException e) {
//            logger.error("Could not create temporary file for converted .lprof file", e);
//            return null;
//        }
//
//        // Convert lprof to json
//        boolean profileFileConversion = convertLprofToJson(
//                project,
//                conversionSdk,
//                profileFile,
//                tempProfileJson
//        );
//
//        // Check success of profilefile json conversion and follow up depending on outcome.
//        if (profileFileConversion) {
//            // Conversion of .lprof file successful, visualize the profile!
//            return fromJson(tempProfileJson.toString());
//        }
//        return null;
//    }

//    /**
//     * Converts a .lprof file to a readable .json
//     *
//     * Also sends notifications to IDE when failures require user action.
//     * Conversion is done with the `load_kernprof_file.py`
//     *
//     * @param project PyCharm project that runs conversion script
//     * @param conversionSdk python sdk to convert lprof with
//     *                      line-profiler-pycharm must be installed to this sdk
//     * @param profileFile lprof file
//     * @param tempProfileJson json output file
//     * @return boolean indicating whether conversion was successful
//     */
//    private static boolean convertLprofToJson(Project project, Sdk conversionSdk, VirtualFile profileFile, Path tempProfileJson) {
//        // Get interpreter path from sdk
//        String interpreterPath = conversionSdk.getHomePath();
//        if (interpreterPath == null) {
//            // interpeter path is not set for python env
//            NotificationGroupManager.getInstance().getNotificationGroup("Colored Lineprofiler Notifications")
//                    .createNotification(
//                            "Python environment " + conversionSdk + " has no interpreter set up.",
//                            NotificationType.ERROR)
//                    .notify(project);
//            logger.error("Python env " + conversionSdk + " has no interpreter set up");
//            return false;
//        }
//
//        // Read the profile conversion script so that it can be run as python `-c` argument
//        InputStream convertProfileScriptStream = Profile.class.getClassLoader().getResourceAsStream("lineprofiler_pycharm/load_kernprof_file.py");
//        assert convertProfileScriptStream != null;
//        String convertProfileScript;
//        try {
//            convertProfileScript = new String(convertProfileScriptStream.readAllBytes(), StandardCharsets.UTF_8);
//        } catch (IOException e) {
//            logger.error("Could not read profile conversion python-script", e);
//            return false;
//        }
//
//        // Setup the command line that will execute the conversion
//        GeneralCommandLine gcl = new GeneralCommandLine()
//                .withWorkDirectory(tempProfileJson.getParent().toString())
//                .withExePath(interpreterPath)
//                .withParameters(
//                        "-c",
//                        convertProfileScript,
//                        profileFile.getPath(),
//                        tempProfileJson.toString());
//
//        // Execute conversion
//        ProcessHandler processHandler;
//        try {
//            processHandler = PythonProcessRunner.createProcess(gcl);
//        } catch (ExecutionException e) {
//            logger.error("Converting " + profileFile + " failed: could not start process", e);
//            return false;
//        }
//        // Wait for conversion completion
//        ProgressManagerImpl.getInstance().runProcessWithProgressSynchronously(
//                () -> {
//                    processHandler.startNotify();
//                    processHandler.waitFor();
//                    logger.warn("Exit code of profile conversion: " + processHandler.getExitCode());
//                },
//                "Converting line profile to json", false, project
//        );
//        Integer convertProfileFileExitCode = processHandler.getExitCode();
//        assert convertProfileFileExitCode != null;
//
//        // Check success of profilefile json conversion and follow up depending on outcome.
//        if (convertProfileFileExitCode == 0) {
//            // Conversion of .lprof file successful
//            return true;
//        } else if (convertProfileFileExitCode == 2) {
//            // Pickle protocol error
//            NotificationGroupManager.getInstance().getNotificationGroup("Colored Lineprofiler Notifications")
//                    .createNotification(
//                            "Could not read " + profileFile.getName() + " because it was created with a conflicting " +
//                                    "version of pickle. Consider changing the Python Environment for " +
//                                    "loading .lprof files in settings (Tools -> Colored Lineprofiler)",
//                            NotificationType.ERROR)
//                    .notify(project);
//        } else {
//            NotificationGroupManager.getInstance().getNotificationGroup("Colored Lineprofiler Notifications")
//                    .createNotification(
//                            "Could not read " + profileFile.getName() + " because of an unknown error",
//                            NotificationType.ERROR)
//                    .notify(project);
//        }
//        return false;
//    }
}
