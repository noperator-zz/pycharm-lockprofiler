package com.jusx.pycharm.lineprofiler.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
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
import com.jusx.pycharm.lineprofiler.profile.Profile;
import com.jusx.pycharm.lineprofiler.render.FunctionProfileInlayRenderer;
import com.jusx.pycharm.lineprofiler.render.LineProfileInlayRenderer;
import com.jusx.pycharm.lineprofiler.render.TableAlignment;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jusx.pycharm.lineprofiler.render.InlayRendererUtils.getFontMetrics;
import static com.jusx.pycharm.lineprofiler.render.InlayRendererUtils.getMargin;

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

    // Project to which this service belongs
    private final Project myProject;

    private final Map<VirtualFile, List<Inlay<? extends EditorCustomElementRenderer>>> inlays = new HashMap<>();
    private final Map<VirtualFile, List<RangeHighlighter>> highlighters = new HashMap<>();
    private final Map<VirtualFile, TimeFractionCalculation> fileTFC = new HashMap<>();
    private @Nullable Profile currentProfile;

    public ProfileHighlightService(Project project) {
        myProject = project;
    }

    /**
     * Returns boolean indicating whether a file has line profiler visualizations that are showing
     * @param file file to check
     */
    public boolean containsVisualizations(VirtualFile file) {
        return highlighters.containsKey(file) || inlays.containsKey(file);
    }

    /**
     * Returns TimeFractionCalculation type that is currently active for a file.
     * @param file file to check
     * @return TimeFractionCalculation, null if no visualizations are currently active
     */
    public TimeFractionCalculation currentTimeFractionCalculation(VirtualFile file) {
        return fileTFC.get(file);
    }

    /**
     * Removes all visualizatios from the project
     */
    public void disposeAllVisualizations() {
        highlighters.forEach((virtualFile, fileHighlighters) -> fileHighlighters.forEach(RangeMarker::dispose));
        highlighters.clear();
        inlays.forEach((virtualFile, fileInlays) -> fileInlays.forEach(Disposable::dispose));
        inlays.clear();
    }

    /**
     * Removes all visualizations from a certain file
     *
     * @param file file to remove all highlighters from
     */
    public void disposeHighlighters(VirtualFile file) {
        List<RangeHighlighter> fileHighlighters = highlighters.get(file);
        if (fileHighlighters != null) {
            fileHighlighters.forEach(RangeMarker::dispose);
        }
        highlighters.remove(file);
        List<Inlay<? extends EditorCustomElementRenderer>> fileInlays = inlays.get(file);
        if (fileInlays != null) {
            fileInlays.forEach(Disposable::dispose);
        }
        inlays.remove(file);
    }

    /**
     * Adds an inlay to the service for management purposes
     */
    private void addInlay(Inlay<? extends EditorCustomElementRenderer> inlay,
                          VirtualFile file) {
        inlays.computeIfAbsent(file, k -> new ArrayList<>()).add(inlay);
    }

    /**
     * Adds a highlighter to the service for managemennt purposes
     */
    private void addHighlighter(RangeHighlighter highlighter, VirtualFile file) {
        highlighters.computeIfAbsent(file, k -> new ArrayList<>()).add(highlighter);
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

        // Check if there are highlighters and inlays for the current file
        List<RangeHighlighter> fileHighlighters = highlighters.get(file);

        List<Inlay<?>> fileInlays = inlays.get(file);
        if (fileHighlighters == null && fileInlays == null) {
            return;
        }
        if (fileHighlighters == null) {
            fileHighlighters = new ArrayList<>();
        }
        if (fileInlays == null) {
            fileInlays = new ArrayList<>();
        }

        // Keep track of a list of disposed highlighters and inlays
        List<RangeHighlighter> disposedHighlighters = new ArrayList<>();
        List<Inlay<?>> disposedInlays = new ArrayList<>();

        // Get all carets that need to be checked for highlight overlap
        CaretModel cm = editor.getCaretModel();
        List<Caret> carets = cm.getAllCarets();

        // Iterate through all carets, test overlap, and dispose overlapping highlights if necessary
        for (Caret caret : carets) {
            int caretStartLine = document.getLineNumber(caret.getSelectionStart());
            int caretEndLine = document.getLineNumber(caret.getSelectionEnd());

            fileHighlighters.forEach(highlighter -> {
                // Test overlap
                if (highlighter.getDocument() != document) { return; }

                int rhLine = document.getLineNumber(highlighter.getStartOffset());

                if (caretStartLine <= rhLine && caretEndLine >= rhLine) {
                    // There is overlap, dispose
                    highlighter.dispose();
                    disposedHighlighters.add(highlighter);
                }
            });
            fileInlays.forEach(inlay -> {
                int inlayLine = document.getLineNumber(inlay.getOffset());
                if (caretStartLine <= inlayLine && caretEndLine >= inlayLine) {
                    // There is overlap, dispose
                    inlay.dispose();
                    disposedInlays.add(inlay);
                }
            });
        }

        // Remove reference to all disposed highlighters
        fileHighlighters.removeAll(disposedHighlighters);
        fileInlays.removeAll(disposedInlays);

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
        disposeAllVisualizations();
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
            disposeAllVisualizations();
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
        if (timeFractionCalculation == TimeFractionCalculation.FUNCTION_TOTAL) {
            timeDenominator = fProfile.getTotalTime();
        } else if (timeFractionCalculation == TimeFractionCalculation.FUNCTION_MAX_LINE_TIME) {
            timeDenominator = fProfile.getMaxLineTime();
        }


        setFunctionProfileVisualizations(fileEditor, fProfile, file, timeDenominator);

        // Set the currently used TimeFractionCalculation
        fileTFC.put(file, timeFractionCalculation);
    }

    private void setFunctionProfileVisualizations(Editor editor,
                                                  FunctionProfile fProfile,
                                                  VirtualFile file, float timeDenominator) {
        // We keep an alignment object that is passed to each render
        // With this alignment object multiple renderers can agree upon the table x offset for results table
        TableAlignment desiredTableAlignment = new TableAlignment();


        InlayModel inlayModel = editor.getInlayModel();
        int offset;

        // Create new inlay for function profile (contains profile meta data))
        FunctionProfileInlayRenderer fRenderer = new FunctionProfileInlayRenderer(
                fProfile,
                currentProfile,
                desiredTableAlignment
        );
        offset = editor.logicalPositionToOffset(new LogicalPosition(fProfile.getLineNrFromZero(), 0));
        Inlay<FunctionProfileInlayRenderer> fInlay = inlayModel.addBlockElement(
                offset, true, true, 100, fRenderer);
        addInlay(fInlay, file);

        // Create new visualizations for line profile
        for (LineProfile line : fProfile.getProfiledLines()) {
            // Highlighter for gutter color
            RangeHighlighter rh = loadLineProfile(
                    editor,
                    line,
                    timeDenominator);
            addHighlighter(rh, file);

            // Inlay for in text table and colormap
            LineProfileInlayRenderer renderer = new LineProfileInlayRenderer(
                    line,
                    timeDenominator,
                    desiredTableAlignment,
                    getMargin(getFontMetrics(editor)));
            offset = editor.logicalPositionToOffset(new LogicalPosition(line.getLineNrFromZero(), 0));
            Inlay<LineProfileInlayRenderer> inlay = inlayModel.addAfterLineEndElement(offset, false, renderer);
            addInlay(inlay, file);
        }
    }

    private RangeHighlighter loadLineProfile(Editor editor, LineProfile lineProfile, float timeDenominator) {
        ColorMapService colorMapService = ServiceManager.getService(ColorMapService.class);

        RangeHighlighter hl = editor.getMarkupModel()
                .addLineHighlighter(
                        null,
                        lineProfile.getLineNrFromZero(),
                        HighlighterLayer.SELECTION
                );

        hl.setLineMarkerRenderer(new DefaultLineMarkerRenderer(
                colorMapService.getTimeFractionTextAttributesKey(lineProfile, timeDenominator), GUTTER_COLOR_THICKNESS));
        return hl;
    }
}

