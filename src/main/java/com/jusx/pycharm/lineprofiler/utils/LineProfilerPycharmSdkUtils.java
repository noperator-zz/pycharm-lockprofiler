package com.jusx.pycharm.lineprofiler.utils;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.python.packaging.PyPackage;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.packaging.PyPackageManagers;

import java.util.List;

public class LineProfilerPycharmSdkUtils {
    private static final Logger logger = Logger.getInstance(LineProfilerPycharmSdkUtils.class.getName());

    /**
     * Checks whether a python sdk has line-profiler-pycharm installed
     * @param sdk sdk to check
     * @return boolean indicating whether line-profiler-pycharm is installed
     */
    public static boolean hasLineProfilerPycharmInstalled(Sdk sdk) throws ExecutionException {
        PyPackageManager ppm = PyPackageManagers.getInstance().forSdk(sdk);
        List<PyPackage> installedPackages = ppm.refreshAndGetPackages(false);

        for (PyPackage p : installedPackages) {
            if (p.getName().equals("line-profiler-pycharm")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests and performs installation of our helper package line-profiler-pycharm in a python environment
     * @param sdk sdk in which to install line-profiler-pycharm
     * @param explanation explanation that is shown when requesting the installation
     * @return boolean indicating whether line-profiler-pycharm was installed
     */
    public static boolean requestAndDoInstallation(Project project, Sdk sdk, String explanation) {
        LineprofilerInstallDialogWrapper dw = new LineprofilerInstallDialogWrapper(sdk, explanation);
        if (dw.showAndGet()) {
            // User gave permission to install line-profiler-pycharm
            PyPackageManager ppm = PyPackageManagers.getInstance().forSdk(sdk);
            var ref = new Object() {
                boolean success = true;
            };

            ProgressManagerImpl.getInstance().runProcessWithProgressSynchronously(
                () -> {
                    try {
                        ppm.install("line-profiler-pycharm");
                    } catch (ExecutionException e) {
                        ref.success = false;
                        logger.error("Error when installing line-profiler-pycharm to environment " + sdk, e);
                    }
                },
                "Installing line-profiler-pycharm in " + sdk.getName(), false, project
            );
            return ref.success;
        } else {
            // User did not give permission to install line profiler
            return false;
        }
    }
}
