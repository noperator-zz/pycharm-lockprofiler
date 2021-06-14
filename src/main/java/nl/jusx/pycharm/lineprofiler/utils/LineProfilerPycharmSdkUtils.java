package nl.jusx.pycharm.lineprofiler.utils;

import com.intellij.execution.ExecutionException;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.python.packaging.PyPackage;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.packaging.PyPackageManagers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineProfilerPycharmSdkUtils {
    private static final Logger logger = Logger.getInstance(LineProfilerPycharmSdkUtils.class.getName());
    private static final Pattern versionPattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)");

    /**
     * Checks whether a python sdk has line-profiler-pycharm installed
     * @param sdk sdk to check
     * @return the line-profiler-pycharm package
     */
    public static PyPackage hasLineProfilerPycharmInstalled(Project project, Sdk sdk) {
        PyPackageManager ppm = PyPackageManagers.getInstance().forSdk(sdk);
        var ref = new Object() {
            List<PyPackage> installedPackages = new ArrayList<>();
        };
        ProgressManagerImpl.getInstance().runProcessWithProgressSynchronously(
                () -> {
                    try {
                        ref.installedPackages = ppm.refreshAndGetPackages(false);
                    } catch (ExecutionException e) {
                        logger.error("Could not check whether line-profiler-pycharm was installed to interpreter " + sdk, e);
                        e.printStackTrace();
                    }
                },
                "Refreshing packages", false, project
        );

        for (PyPackage p : ref.installedPackages) {
            if (p.getName().equals("line-profiler-pycharm")) {
                return p;
            }
        }
        return null;
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
                        ppm.install("line-profiler-pycharm==1.1.0");
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

    private static boolean wrapRequestAndDoInstallation(Project project, Sdk sdk, String explanation) {
        boolean installed = requestAndDoInstallation(
                project,
                sdk,
                explanation
        );
        if (!installed) {
            NotificationGroupManager.getInstance().getNotificationGroup("Line Profiler Notifications")
                    .createNotification(
                            "Package 'line-profiler-pycharm' was not installed to " + sdk,
                            NotificationType.WARNING)
                    .notify(project);
            return false;
        }
        return true;
    }

    /**
     * Ensures the installation of `line-profiler-pycharm` in a python interpreter
     *
     * First it is checked whether the package is already installed
     * If not, we request the user to install the package
     *
     * @param project project
     * @param sdk interpeter to check line-profiler-pycharm installation on
     * @return boolean indicating whether package is installed
     */
    public static boolean ensureLineProfilerPycharmPackageInstalled(Project project, Sdk sdk) {
        // Check whether lineprofiler is installed
        PyPackage p = hasLineProfilerPycharmInstalled(project, sdk);

        if (Objects.isNull(p)) {
            return wrapRequestAndDoInstallation(project, sdk,
                    "Package 'line-profiler-pycharm' is not installed in python interpreter.");
        } else if (!versionIsGreaterThanOrEqualTo(p.getVersion(), 1, 1, 0)) {
            return wrapRequestAndDoInstallation(project, sdk,
                    "A newer version of python-package 'line-profiler-pycharm' is available " +
                            "and should be installed to your python environment.");
        }
        return true;
    }

    private static boolean versionIsGreaterThanOrEqualTo(String version,
                                                         int minMajor, int minMinor, int minHotfix)
            throws IllegalArgumentException {
        Matcher m = versionPattern.matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException("Could not parse version " + version);
        }
        int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        int hotfix = Integer.parseInt(m.group(3));
        if (major > minMajor) {
            return true;
        } else if (major == minMajor && minor > minMinor) {
            return true;
        } else return major == minMajor && minor == minMinor && hotfix >= minHotfix;
    }
}
