package nl.jusx.pycharm.lineprofiler.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.DefaultLineMarkerRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.LineMarkerRendererEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import nl.jusx.pycharm.lineprofiler.profile.FunctionProfile;
import nl.jusx.pycharm.lineprofiler.profile.LineProfile;
import nl.jusx.pycharm.lineprofiler.profile.Profile;
import nl.jusx.pycharm.lineprofiler.render.FunctionProfileInlayRenderer;
import nl.jusx.pycharm.lineprofiler.render.LineProfileInlayRenderer;
import nl.jusx.pycharm.lineprofiler.render.ProfileLineMarkerRenderer;
import nl.jusx.pycharm.lineprofiler.render.TableAlignment;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

import static nl.jusx.pycharm.lineprofiler.render.InlayRendererUtils.getFontMetrics;
import static nl.jusx.pycharm.lineprofiler.render.InlayRendererUtils.getMargin;

final class FileProfileData {
    public final Editor editor;
    public final List<Inlay<? extends EditorCustomElementRenderer>> inlays = new ArrayList<>();
    public final List<RangeHighlighter> highlighters = new ArrayList<>();
    public final TableAlignment desiredTableAlignment = new TableAlignment();
    // We keep an alignment object that is passed to each render
    // With this alignment object multiple renderers can agree upon the table x offset for results table
    public TimeFractionCalculation fileTFC = TimeFractionCalculation.FUNCTION_TOTAL;
    public long fileTime = 0;

    FileProfileData(Editor editor) {
        this.editor = editor;
    }
}

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

    private final Map<VirtualFile, FileProfileData> fileData = new HashMap<>();
    private @Nullable Profile currentProfile;

    public ProfileHighlightService(Project project) {
        myProject = project;
    }

    /**
     * Returns boolean indicating whether a file has line profiler visualizations that are showing
     * @param file file to check
     */
    public boolean containsVisualizations(VirtualFile file) {
        return fileData.containsKey(file);
    }

    /**
     * Returns TimeFractionCalculation type that is currently active for a file.
     * @param file file to check
     * @return TimeFractionCalculation, null if no visualizations are currently active
     */
    public TimeFractionCalculation currentTimeFractionCalculation(VirtualFile file) {
        return fileData.get(file).fileTFC;
    }

    /**
     * Removes all visualizatios from the project
     */
    public void disposeAllVisualizations() {
        fileData.forEach((virtualFile, data) -> {
            data.highlighters.forEach(RangeMarker::dispose);
            data.inlays.forEach(Disposable::dispose);
        });
        fileData.clear();
    }

    /**
     * Removes all visualizations from a certain file
     *
     * @param file file to remove all highlighters from
     */
    public void disposeVisualizations(VirtualFile file) {
        // TODO should this function (and friends) explicitly remove the highlighters and inlays from the MarkupModel?
        FileProfileData data = fileData.get(file);
        if (data == null) {
            return;
        }
        data.highlighters.forEach(RangeMarker::dispose);
        data.inlays.forEach(Disposable::dispose);
        fileData.remove(file);
    }

    /**
     * Adds an inlay to the service for management purposes
     */
    private void addInlay(Inlay<? extends EditorCustomElementRenderer> inlay,
                          FileProfileData data) {
        data.inlays.add(inlay);
    }

    /**
     * Adds a highlighter to the service for managemennt purposes
     */
    private void addHighlighter(RangeHighlighter highlighter, FileProfileData data) {
        data.highlighters.add(highlighter);
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
        List<RangeHighlighter> fileHighlighters = fileData.get(file).highlighters;
        List<Inlay<?>> fileInlays = fileData.get(file).inlays;

        if (fileHighlighters == null && fileInlays == null) {
            return;
        }
        if (fileHighlighters == null) {
            fileHighlighters = new ArrayList<>();
        }
        if (fileInlays == null) {
            fileInlays = new ArrayList<>();
        }

        // Keep track of a list highlighters and inlays to dispose
        Set<RangeHighlighter> toDisposeHighlighters = new HashSet<>();
        Set<Inlay<?>> toDisposeInlays = new HashSet<>();

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
                    toDisposeHighlighters.add(highlighter);
                }
            });
            fileInlays.forEach(inlay -> {
                int inlayLine = document.getLineNumber(inlay.getOffset());
                if (caretStartLine <= inlayLine && caretEndLine >= inlayLine) {
                    // There is overlap, dispose
                    toDisposeInlays.add(inlay);
                }
            });
        }

        toDisposeHighlighters.forEach(RangeMarker::dispose);
        toDisposeInlays.forEach(Disposable::dispose);

        // Remove reference to all disposed highlighters
        fileHighlighters.removeAll(toDisposeHighlighters);
        fileInlays.removeAll(toDisposeInlays);

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
        if (currentProfile == null) {
            logger.error("No profile was given to load");
            return;
        }

        // Dispose all existing highlighters because we will load new profile results
        disposeAllVisualizations();

        currentProfile.getProfiledFiles().forEach((fileName, fileProfile) -> {
            VirtualFileManager vfm = VirtualFileManager.getInstance();
            VirtualFile file = vfm.findFileByNioPath(Paths.get(fileName));
            if (file == null) {
                logger.warn("Could not find file: " + fileName);
                return;
            }

            OpenFileDescriptor ofd = new OpenFileDescriptor(myProject, file);
            Editor fileEditor = FileEditorManager.getInstance(myProject).openTextEditor(ofd, true);
            if (fileEditor == null) {
                logger.error("Could not open file in editor: " + fileName);
                return;
            }

            FileProfileData data = new FileProfileData(fileEditor);
            fileData.put(file, data);

            // Compute total time spend in the file for the per-function color bar
            long fileTotal = 0;
            for (FunctionProfile fProfile : fileProfile) {
                fileTotal += fProfile.getTotalTime();
            }

            data.fileTime = fileTotal;

            for (FunctionProfile fProfile : fileProfile) {
                loadFunctionProfile(data, fProfile, timeFractionCalculation);
            }
        });

    }

    private void loadFunctionProfile(FileProfileData data, FunctionProfile fProfile,
                                     TimeFractionCalculation timeFractionCalculation) {
        if (currentProfile == null) {
            logger.error("Could not find a current profile, so could not load function profile");
            return;
        }

        long timeDenominator = 0;
        if (timeFractionCalculation == TimeFractionCalculation.FUNCTION_TOTAL) {
            timeDenominator = fProfile.getTotalTime();
        } else if (timeFractionCalculation == TimeFractionCalculation.FUNCTION_MAX_LINE_TIME) {
            timeDenominator = fProfile.getMaxLineTime();
        }

        setFunctionProfileVisualizations(data, fProfile, timeDenominator);

        // Set the currently used TimeFractionCalculation
        data.fileTFC = timeFractionCalculation;
    }

    private void setFunctionProfileVisualizations(FileProfileData data,
                                                  FunctionProfile fProfile, long timeDenominator) {



        InlayModel inlayModel = data.editor.getInlayModel();
        int offset;

        // On single line functions the function inlay would overlap with the line inlay, so don't render it
        if (fProfile.getNumLines() > 1) {
            // Create new inlay for function profile (contains profile metadata)
            FunctionProfileInlayRenderer fRenderer = new FunctionProfileInlayRenderer(
                    fProfile,
                    data.desiredTableAlignment,
                    getMargin(getFontMetrics(data.editor)),
                    data.fileTime
            );
            offset = data.editor.logicalPositionToOffset(new LogicalPosition(fProfile.getLineNrFromZero(), 0));
            Inlay<FunctionProfileInlayRenderer> fInlay = inlayModel.addAfterLineEndElement(
                    offset, true, fRenderer);
            addInlay(fInlay, data);
        }


        // Highlighter for gutter color
        AddFunctionGutter(
                data.editor,
                fProfile,
                data);
////            addHighlighter(rh, file);

        // Create new visualizations for line profile
        for (LineProfile line : fProfile.getProfiledLines()) {

            // Inlay for in text table and colormap
            LineProfileInlayRenderer renderer = new LineProfileInlayRenderer(
                    line,
                    timeDenominator,
                    data.desiredTableAlignment,
                    getMargin(getFontMetrics(data.editor)));
            offset = data.editor.logicalPositionToOffset(new LogicalPosition(line.getLineNrFromZero(), 0));
            Inlay<LineProfileInlayRenderer> inlay = inlayModel.addAfterLineEndElement(offset, true, renderer);
            addInlay(inlay, data);
        }
    }

    private void AddFunctionGutter(Editor editor, FunctionProfile func, FileProfileData data) {
        ColorMapService colorMapService = ApplicationManager.getApplication().getService(ColorMapService.class);

        double timeFraction = (double) func.getTotalTime() / data.fileTime;
        TextAttributesKey timeColorAttributes = colorMapService.getTimeFractionTextAttributesKey(timeFraction);
        Color timeColor = editor.getColorsScheme().getAttributes(timeColorAttributes).getBackgroundColor();

        ProfileLineMarkerRenderer renderer = new ProfileLineMarkerRenderer(editor, timeColorAttributes, 8, 0, LineMarkerRendererEx.Position.LEFT);

        for (int l = func.getLineNrFromZero(); l < func.getLineNrFromZero() + func.getNumLines(); l++) {
            RangeHighlighter hl = editor.getMarkupModel()
                    .addLineHighlighter(
                            null,
                            l,
                            HighlighterLayer.SELECTION
                    );

            // Set colors in gutter
            hl.setLineMarkerRenderer(renderer);
            // Set colors in scrollbar
            hl.setErrorStripeMarkColor(timeColor);
            hl.setErrorStripeTooltip(null);
            hl.setThinErrorStripeMark(true);
            addHighlighter(hl, data);
        }
    }
}

