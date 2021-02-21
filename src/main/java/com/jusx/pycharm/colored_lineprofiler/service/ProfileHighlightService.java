package com.jusx.pycharm.colored_lineprofiler.service;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jetbrains.python.run.PythonProcessRunner;
import com.jusx.pycharm.colored_lineprofiler.profile.FunctionProfile;
import com.jusx.pycharm.colored_lineprofiler.profile.LineProfile;
import com.jusx.pycharm.colored_lineprofiler.profile.Profile;
import com.jusx.pycharm.colored_lineprofiler.profile.ProfileSchema;
import com.jusx.pycharm.colored_lineprofiler.utils.LineProfilerSdkUtils;
import jViridis.ColorMap;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IntelliJ Platform Service that contains all profile highlights.
 *
 * Profile highlights are highlights that originate from this plugin.
 * They describe and visualise computation time for lines of code of python methods
 */
@Service
public final class ProfileHighlightService {
    private static final Logger logger = Logger.getInstance(ProfileHighlightService.class.getName());
    private static final int AMOUNT_OF_COLORS = 40;

    // Project to which this service belongs
    private final Project myProject;

    // All possible colors that can be used as highlight background
    private final Map<Integer, TextAttributes> lineBackgroundColorMap = new HashMap<>();
    // Highlighters, ordered per file
    private final Map<VirtualFile, List<RangeHighlighter>> highlighters = new HashMap<>();

    private TimeFractionCalculation timeFractionCalculation = TimeFractionCalculation.FUNCTION_TOTAL;
    private @Nullable Profile currentProfile;

    public ProfileHighlightService(Project project) {
        myProject = project;

        loadBackgroundColors();
    }

    /**
     * Loads all colors that can be used as highlight background color
     */
    private void loadBackgroundColors() {
        ColorMap cm = ColorMap.getInstance(ColorMap.VIRIDIS);

        for (int i = 0; i < AMOUNT_OF_COLORS; i++) {
            TextAttributes ta = new TextAttributes();

            float colorFrac = ((float) i) / ((float) AMOUNT_OF_COLORS - 1);
            ta.setBackgroundColor(cm.getColor(colorFrac));
            lineBackgroundColorMap.put(i, ta);
        }
    }

    /**
     * Removes all highlighters from the project
     */
    public void disposeAllHighlighters() {
        for (List<RangeHighlighter> fileHighlighters : highlighters.values()) {
            for (RangeHighlighter rh : fileHighlighters) {
                rh.dispose();
            }
        }
        highlighters.clear();
    }

    /**
     * Removes all highlighters from a certain file
     *
     * @param file file to remove all highlighters from
     */
    public void disposeHighlighters(VirtualFile file) {
        List<RangeHighlighter> fileHighlighters = highlighters.get(file);
        if (fileHighlighters == null) {
            return;
        }
        for (RangeHighlighter rh : fileHighlighters) {
            rh.dispose();
        }
        highlighters.remove(file);
    }

    /**
     * Disposes all highlighters that currently overlap with caret positions
     *
     * This can be used to remove profile highlighters when typing on a highlighted line.
     * That way a line is not highlighted anymore when it is altered which is intuitive because the
     * profile result does then not represent that changed / new line of code anymore
     *
     * @param editor editor in which to remove highlighters overlapping with caret positions
     */
    public void disposeHighlightersOverlappingAtCaretPositions(Editor editor) {
        // Get document and virtualfile
        Document document = editor.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);

        // Check if there are highlighters for the current file, if not return
        List<RangeHighlighter> fileHighlighters = highlighters.get(file);
        if (fileHighlighters == null) {
            return;
        }

        // Keep track of a list of disposed highlighters
        List<RangeHighlighter> disposed = new ArrayList<>();

        // Get all carets that need to be checked for highlight overlap
        CaretModel cm = editor.getCaretModel();
        List<Caret> carets = cm.getAllCarets();

        // Iterate through all carets, test overlap, and dispose overlapping highlights if necessary
        for (Caret caret : carets) {
            int caretStartLine = document.getLineNumber(caret.getSelectionStart());
            int caretEndLine = document.getLineNumber(caret.getSelectionEnd());

            for (RangeHighlighter rh : fileHighlighters) {
                // Test overlap
                if (rh.getDocument() != document) { continue; }

                int rhLine = document.getLineNumber(rh.getStartOffset());

                if (caretStartLine <= rhLine && caretEndLine >= rhLine) {
                    // There is overlap, dispose
                    rh.dispose();
                    disposed.add(rh);
                }
            }
        }

        // Remove reference to all disposed highlighters
        fileHighlighters.removeAll(disposed);
    }

    private void loadLineProfileJson(String fileName) {
        // Dispose all existing highlighters because we will load new profile results
        disposeAllHighlighters();

        Gson gson = new Gson();
        JsonReader reader;

        try {
            reader = new JsonReader(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        ProfileSchema data = gson.fromJson(reader, ProfileSchema.class); // contains the whole reviews list
        Profile profile = new Profile(data);

        loadProfile(profile);
    }

    private void loadProfile(Profile p) {
        currentProfile = p;

        for (FunctionProfile functionProfile : p.getProfiledFunctions()) {
            loadFunctionProfile(functionProfile);
        }
    }

    private void loadFunctionProfile(FunctionProfile fProfile) {
        VirtualFileManager vfm = VirtualFileManager.getInstance();
        VirtualFile file = vfm.findFileByNioPath(Paths.get(fProfile.getFile()));
        if (file == null) {
            logger.warn("Could not find file: " + fProfile.getFile());
            return;
        }
        OpenFileDescriptor ofd = new OpenFileDescriptor(myProject, file);
        Editor fileEditor = FileEditorManager.getInstance(myProject).openTextEditor(ofd, true);
        if (fileEditor == null) {
            logger.error("Could not open file in editor: " + fProfile.getFile());
            return;
        }

        if (currentProfile == null) {
            logger.error("Could ont find a current profile, so could not load function profile");
            return;
        }

        float timeDenominator = 0;
        if (timeFractionCalculation == TimeFractionCalculation.PROFILE_TOTAL) {
            timeDenominator = currentProfile.getTotalTime();
        } else if (timeFractionCalculation == TimeFractionCalculation.FUNCTION_TOTAL) {
            timeDenominator = fProfile.getTotalTime();
        } else if (timeFractionCalculation == TimeFractionCalculation.FUNCTION_MAX_LINE_TIME) {
            timeDenominator = fProfile.getMaxLineTime();
        }

        List<RangeHighlighter> fileHighlighters = highlighters.computeIfAbsent(file, k -> new ArrayList<>());

        for (LineProfile line : fProfile.getProfiledLines()) {
            RangeHighlighter rh = loadLineProfile(
                    fileEditor,
                    line,
                    timeDenominator);
            fileHighlighters.add(rh);
        }
    }

    private RangeHighlighter loadLineProfile(Editor editor, LineProfile lineProfile, float timeDenominator) {
        int lineNo = lineProfile.getLineNrFromZero();
        float timeFraction = lineProfile.getTime() / timeDenominator;

        int colorIndex = (int) (timeFraction * (float) (AMOUNT_OF_COLORS - 1));

        return editor.getMarkupModel()
                .addLineHighlighter(
                        lineNo,
                        HighlighterLayer.CARET_ROW - 1,
                        lineBackgroundColorMap.get(colorIndex));
    }

    public void loadLineProfile(Sdk conversionSdk, VirtualFile profileFile) {
        // Get interpreter path from sdk
        String interpreterPath = conversionSdk.getHomePath();
        if (interpreterPath == null) {
            // TODO send notification?
            logger.error("Python env " + conversionSdk + " has no interpreter set up");
            return;
        }

        // Check whether lineprofiler is installed
        boolean lineProfilerInstalled;
        try {
            lineProfilerInstalled = LineProfilerSdkUtils.hasLineProfilerInstalled(conversionSdk);
        } catch (ExecutionException e) {
            logger.error("Could not check whether line-profiler was installed to sdk " + conversionSdk, e);
            return;
        }
        if (!lineProfilerInstalled) {
            lineProfilerInstalled = LineProfilerSdkUtils.requestAndDoInstallation(
                    myProject,
                    conversionSdk,
                    "Package line-profiler is not installed to python environment."
            );
            if (!lineProfilerInstalled) {
                logger.warn("Package installation line-profiler to sdk " + conversionSdk + " did not succeed");
                return;
            }
        }

        // Create temporary file to which readable profile json will be written.
        Path tempProfileJson;
        try {
            tempProfileJson = Files.createTempFile(profileFile.getName() + "-converted", ".json");
        } catch (IOException e) {
            logger.error("Could not create temporary file for converted .lprof file", e);
            return;
        }

        InputStream convertProfileScriptStream = getClass().getClassLoader().getResourceAsStream("load_kernprof_file.py");
        assert convertProfileScriptStream != null;
        String convertProfileScript;
        try {
            convertProfileScript = new String(convertProfileScriptStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Could not read profile conversion python-script", e);
            return;
        }

        GeneralCommandLine gcl = new GeneralCommandLine()
                .withWorkDirectory(tempProfileJson.getParent().toString())
                .withExePath(interpreterPath)
                .withParameters(
                        "-c",
                        convertProfileScript,
                        profileFile.getPath(),
                        tempProfileJson.toString());

        ProcessHandler processHandler;
        try {
            processHandler = PythonProcessRunner.createProcess(gcl);
        } catch (ExecutionException e) {
            logger.error("Converting " + profileFile + " failed: could not start process", e);
            return;
        }

        ProgressManagerImpl.getInstance().runProcessWithProgressSynchronously(
                () -> {
                    // TODO what if there is no line_profiler installed?
                    // TODO what if wrong protocol version
                    processHandler.startNotify();
                    processHandler.waitFor();
                    logger.warn("Exit code of profile conversion: " + processHandler.getExitCode());
                },
                "Converting line profile to json", false, myProject
        );


        // TODO check protocol version error

        loadLineProfileJson(tempProfileJson.toString());
    }
}

