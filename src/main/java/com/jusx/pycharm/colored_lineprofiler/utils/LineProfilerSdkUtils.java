package com.jusx.pycharm.colored_lineprofiler.utils;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.python.packaging.PyPackage;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.packaging.PyPackageManagers;

import java.util.List;

public class LineProfilerSdkUtils {
    private static final Logger logger = Logger.getInstance(LineProfilerSdkUtils.class.getName());

    /**
     * Checks whether a python sdk has line-profiler installed
     * @param sdk sdk to check
     * @return boolean indicating whether line-profiler is installed
     */
    public static boolean hasLineProfilerInstalled(Sdk sdk) throws ExecutionException {
        PyPackageManager ppm = PyPackageManagers.getInstance().forSdk(sdk);
        List<PyPackage> installedPackages = ppm.refreshAndGetPackages(false);

        for (PyPackage p : installedPackages) {
            if (p.getName().equals("line-profiler")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests and performs installation of line-profiler in a python environment
     * @param sdk sdk in which to install line-profiler
     * @param explanation explanation that is shown when requesting the installation
     * @return boolean indicating whether line-profiler was installed
     * @throws ExecutionException when installation fails
     */
    public static boolean requestAndDoInstallation(Project project, Sdk sdk, String explanation) {
        LineprofilerInstallDialogWrapper dw = new LineprofilerInstallDialogWrapper(sdk, explanation);
        if (dw.showAndGet()) {
            // User gave permission to install line-profiler
            PyPackageManager ppm = PyPackageManagers.getInstance().forSdk(sdk);
            var ref = new Object() {
                boolean success = true;
            };

            ProgressManagerImpl.getInstance().runProcessWithProgressSynchronously(
                () -> {
                    try {
                        ppm.install("line-profiler");
                    } catch (ExecutionException e) {
                        ref.success = false;
                        logger.error("Error when installing line-profiler to environment " + sdk, e);
                    }
                },
                "Installing line-profiler in " + sdk.getName(), false, project
            );
            return ref.success;
        } else {
            // User did not give permission to install line profiler
            return false;
        }
    }
}
