package com.jusx.pycharm.lineprofiler.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.DefaultLineMarkerRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jusx.pycharm.lineprofiler.profile.FunctionProfile;
import com.jusx.pycharm.lineprofiler.profile.LineProfile;
import com.jusx.pycharm.lineprofiler.profile.LineProvider;
import com.jusx.pycharm.lineprofiler.profile.Profile;
import com.jusx.pycharm.lineprofiler.render.FunctionProfileHighlighterRenderer;
import com.jusx.pycharm.lineprofiler.render.LineProfileHighlighterRenderer;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
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
    private static final int GUTTER_COLOR_THICKNESS = 10;
    private static final int MAX_COLUMN_ALIGNMENT_RESULTS_RENDER = 75;  // columns

    // Project to which this service belongs
    private final Project myProject;

    // Highlighters, ordered per file
    private final Map<VirtualFile, List<RangeHighlighter>> highlighters = new HashMap<>();
    private final Map<VirtualFile, TimeFractionCalculation> fileTFC = new HashMap<>();
    private @Nullable Profile currentProfile;

    public ProfileHighlightService(Project project) {
        myProject = project;
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


        // Keep track of the end offset (end of text covered by highlighter) for each line
        // With this we can determine a desired alignment for our profile result visualizations.
        // We do maintain a maximum for the desired X position. Lines with text that exceed this max x position will
        // have results that break alignment
        int mostRightColumn = getLineEndColumn(fileEditor, fProfile).column;
        for (LineProfile line : fProfile.getProfiledLines()) {
            int checkColumn = getLineEndColumn(fileEditor, line).column;
            if (checkColumn > mostRightColumn && checkColumn <= MAX_COLUMN_ALIGNMENT_RESULTS_RENDER) {
                mostRightColumn = checkColumn;
            }
        }

        // Create renderers
        EditorColorsScheme scheme = fileEditor.getColorsScheme();
        int fontSize = scheme.getEditorFontSize();
        Font resultsRendererFont = scheme.getFont(EditorFontType.ITALIC).deriveFont(fontSize * 0.9f);
        int desiredXAlignment = fileEditor.logicalPositionToXY(new LogicalPosition(0, mostRightColumn)).x;
        FunctionProfileHighlighterRenderer functionProfileHighlighterRenderer = new FunctionProfileHighlighterRenderer(
                DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT_HIGHLIGHTED,
                resultsRendererFont,
                desiredXAlignment
        );
        LineProfileHighlighterRenderer lineProfileHighlighterRenderer = new LineProfileHighlighterRenderer(
                DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT_HIGHLIGHTED,
                resultsRendererFont,
                desiredXAlignment,
                timeDenominator
        );

        // Keep track of the new highlighters
        List<RangeHighlighter> fileHighlighters = highlighters.computeIfAbsent(file, k -> new ArrayList<>());

        // Create new highlighter for function profile (header highlighter)
        RangeHighlighter fProfHighlighter = getHighlighter(fileEditor, fProfile);
        fProfHighlighter.setCustomRenderer(functionProfileHighlighterRenderer);
        fileHighlighters.add(fProfHighlighter);

        // Create new highlighters for line profile
        for (LineProfile line : fProfile.getProfiledLines()) {
            RangeHighlighter rh = loadLineProfile(
                    fileEditor,
                    line,
                    timeDenominator);
            // add profile to renderer for lookup purposes
            lineProfileHighlighterRenderer.addLineProfile(rh, line);
            // set renderer to range highlighter
            rh.setCustomRenderer(lineProfileHighlighterRenderer);
            // Track highlighter for management purposes
            fileHighlighters.add(rh);
        }

        // Set the currently used TimeFractionCalculation
        fileTFC.put(file, timeFractionCalculation);
    }

    /**
     * Returns logical position of last character on a line
     * @param editor editor to check line in
     * @param line line nr to check last position
     */
    private LogicalPosition getLineEndColumn(Editor editor, LineProvider line) {
        int lineEndOffset = editor.getDocument().getLineEndOffset(line.getLineNrFromZero());
        return editor.offsetToLogicalPosition(lineEndOffset);
    }

    private RangeHighlighter getHighlighter(Editor editor, LineProvider line) {
        return editor.getMarkupModel()
                .addLineHighlighter(
                        null,
                        line.getLineNrFromZero(),
                        HighlighterLayer.SELECTION
                );
    }

    private RangeHighlighter loadLineProfile(Editor editor, LineProfile lineProfile, float timeDenominator) {
        ColorMapService colorMapService = ServiceManager.getService(ColorMapService.class);

        RangeHighlighter hl = getHighlighter(editor, lineProfile);

        hl.setLineMarkerRenderer(new DefaultLineMarkerRenderer(
                colorMapService.getTimeFractionTextAttributesKey(lineProfile, timeDenominator), GUTTER_COLOR_THICKNESS));
        return hl;
    }
}

