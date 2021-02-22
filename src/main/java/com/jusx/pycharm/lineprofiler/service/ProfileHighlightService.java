package com.jusx.pycharm.lineprofiler.service;

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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jusx.pycharm.lineprofiler.profile.FunctionProfile;
import com.jusx.pycharm.lineprofiler.profile.LineProfile;
import com.jusx.pycharm.lineprofiler.profile.Profile;
import jViridis.ColorMap;
import org.jetbrains.annotations.Nullable;

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
    private final Map<VirtualFile, TimeFractionCalculation> fileTFC = new HashMap<>();
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
     * Returns boolean indicating whether highlighters are active for a file
     * @param file file to check
     */
    public boolean containsHighlights(VirtualFile file) {
        return highlighters.containsKey(file);
    }

    /**
     * Returns boolean indicating whether highlighters are active for a file
     * @param file file to check
     */
    public TimeFractionCalculation currentTimeFractionCalculation(VirtualFile file) {
        return fileTFC.get(file);
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

    /**
     * Registers a new profile as profile for this highlight service
     *
     * After registering the profile, one can invoke `visualizeProfile`
     * to create the visualizations
     *
     * @param profile profile to register
     */
    public void setProfile(Profile profile) {
        disposeAllHighlighters();
        currentProfile = profile;
    }

    /**
     * Visualizes the registered profile in files of this project
     *
     * @param timeFractionCalculation method to calculate the time fraction
     */
    public void visualizeProfile(TimeFractionCalculation timeFractionCalculation) {
        visualizeProfile(timeFractionCalculation, null);
    }

    public void visualizeProfile(TimeFractionCalculation timeFractionCalculation, VirtualFile forFile) {
        if (forFile == null) {
            // Dispose all existing highlighters because we will load new profile results
            disposeAllHighlighters();
        } else {
            // Dispose all existing highlighters only for the given VirtualFile because we will load
            // new profile results for that file
            disposeHighlighters(forFile);
        }
        if (currentProfile == null) {
            logger.error("No profile was given to load");
            return;
        }
        for (FunctionProfile functionProfile : currentProfile.getProfiledFunctions()) {
            loadFunctionProfile(functionProfile, timeFractionCalculation, forFile);
        }
    }

    private void loadFunctionProfile(FunctionProfile fProfile, TimeFractionCalculation timeFractionCalculation, @Nullable VirtualFile forFile) {
        VirtualFileManager vfm = VirtualFileManager.getInstance();
        VirtualFile file = vfm.findFileByNioPath(Paths.get(fProfile.getFile()));
        if (file == null) {
            logger.warn("Could not find file: " + fProfile.getFile());
            return;
        }
        if (forFile != null && !file.equals(forFile)) {
            // We only want to load profiles for forFile if forFile is not null
            return;
        }
        OpenFileDescriptor ofd = new OpenFileDescriptor(myProject, file);
        Editor fileEditor = FileEditorManager.getInstance(myProject).openTextEditor(ofd, true);
        if (fileEditor == null) {
            logger.error("Could not open file in editor: " + fProfile.getFile());
            return;
        }

        if (currentProfile == null) {
            logger.error("Could not find a current profile, so could not load function profile");
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

        // Set the currently used TimeFractionCalculation
        fileTFC.put(file, timeFractionCalculation);
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
}

