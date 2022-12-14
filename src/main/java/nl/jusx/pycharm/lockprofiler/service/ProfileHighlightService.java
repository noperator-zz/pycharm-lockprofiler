package nl.jusx.pycharm.lockprofiler.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import nl.jusx.pycharm.lockprofiler.profile.Profile;
import nl.jusx.pycharm.lockprofiler.profile.ProfileSchema;
import nl.jusx.pycharm.lockprofiler.render.*;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.List;
import java.util.*;

import static nl.jusx.pycharm.lockprofiler.render.InlayRendererUtils.getFontMetrics;
import static nl.jusx.pycharm.lockprofiler.render.InlayRendererUtils.getMargin;

final class FileProfileData {
    public final Editor editor;
    public final List<Inlay<? extends EditorCustomElementRenderer>> inlays = new ArrayList<>();
    public final List<RangeHighlighter> highlighters = new ArrayList<>();
    public final TableAlignment desiredTableAlignment = new TableAlignment();
    // We keep an alignment object that is passed to each render
    // With this alignment object multiple renderers can agree upon the table x offset for results table
    public TimeFractionCalculation fileTFC = TimeFractionCalculation.FUNCTION_TOTAL;

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
    private static final int GUTTER_COLOR_THICKNESS = 9;

    // Project to which this service belongs
    private final Project myProject;

    private final Map<VirtualFile, FileProfileData> fileData = new HashMap<>();
    private @Nullable Profile currentProfile;
    private LockProfilerToolWindow lockProfilerToolWindow;

    public ProfileHighlightService(Project project) {
        myProject = project;

        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow("Lock Profiler");
        String contentName = "Lock Profiler";//profileFile.getName();

        if (toolWindow != null) {
            toolWindow.setToHideOnEmptyContent(true);
            lockProfilerToolWindow = new LockProfilerToolWindow(toolWindow, project);
            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            Content content = contentFactory.createContent(lockProfilerToolWindow.getContent(), contentName, false);
            toolWindow.getContentManager().addContent(content);
        }
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
        FileProfileData data = fileData.get(file);
        if (data == null) {
            return;
        }

        List<RangeHighlighter> fileHighlighters = data.highlighters;
        List<Inlay<?>> fileInlays = data.inlays;

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
        visualizeProfile(TimeFractionCalculation.FUNCTION_TOTAL);

        lockProfilerToolWindow.update(profile);
        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow("Lock Profiler");

        if (toolWindow != null) {
            toolWindow.show();
            toolWindow.activate(null, true);
        }
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

        currentProfile.getProfiledFiles().forEach((fileName, file_stats) -> {
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

//            System.out.print("start\n");
            loadFileProfile(data, file_stats, timeFractionCalculation);

//            // TODO force redraw editor to to redraw inlays with the latest tableAlignment position.
//            //  Or if that's not possible, do ao first pass to compute the correct tableAlignment before creating the inlays
//            System.out.print("repaint\n");
//            System.out.printf("%d\n", data.desiredTableAlignment.getX());
//            data.inlays.forEach(Inlay::repaint);
//            System.out.printf("%d\n", data.desiredTableAlignment.getX());
//            data.inlays.forEach(Inlay::repaint);
//            System.out.printf("%d\n", data.desiredTableAlignment.getX());
//            System.out.print("repaint done\n");
        });

    }

    private void loadFileProfile(FileProfileData data, Map<Integer, Map<Long, ProfileSchema.LockStats>> line_stats,
                                 TimeFractionCalculation timeFractionCalculation) {


        InlayModel inlayModel = data.editor.getInlayModel();
//        long timeDenominator = 0;
//        if (timeFractionCalculation == TimeFractionCalculation.FUNCTION_TOTAL) {
//            timeDenominator = fProfile.getTotalTime();
//        } else if (timeFractionCalculation == TimeFractionCalculation.FUNCTION_MAX_LINE_TIME) {
//            timeDenominator = fProfile.getMaxLineTime();
//        }

        // Set the currently used TimeFractionCalculation
        data.fileTFC = timeFractionCalculation;

        line_stats.forEach((lineNo, lock_stats) -> {
            int offset;

            // Highlighter for gutter color
//            AddFunctionGutter(data, percentage);

            // Inlay for in text table and colormap
            LineProfileInlayRenderer renderer = new LineProfileInlayRenderer(
                    currentProfile.schema,
                    lineNo,
                    lock_stats,
//                    timeDenominator,
                    data.desiredTableAlignment,
                    getMargin(getFontMetrics(data.editor))
            );

            offset = data.editor.logicalPositionToOffset(new LogicalPosition(lineNo - 1, 0));
            Inlay<LineProfileInlayRenderer> inlay = inlayModel.addAfterLineEndElement(offset, true, renderer);
            addInlay(inlay, data);
        });
    }

//    private void AddFunctionGutter(Editor editor, FunctionProfile func, FileProfileData data) {
//        ColorMapService colorMapService = ApplicationManager.getApplication().getService(ColorMapService.class);
//
//        double timeFraction = (double) func.getTotalTime() / data.fileTime;
//        TextAttributesKey timeColorAttributes = colorMapService.getTimeFractionTextAttributesKey(timeFraction);
//        Color timeColor = editor.getColorsScheme().getAttributes(timeColorAttributes).getBackgroundColor();
//
//        ProfileLineMarkerRenderer renderer = new ProfileLineMarkerRenderer(editor, timeColorAttributes,
//                GUTTER_COLOR_THICKNESS, 0, LineMarkerRendererEx.Position.LEFT);
//
////        for (int l = func.getLineNrFromZero(); l < func.getLineNrFromZero() + func.getNumLines(); l++) {
//            RangeHighlighter hl = editor.getMarkupModel()
//                    .addLineHighlighter(
//                            null,
//                            l,
//                            HighlighterLayer.SELECTION
//                    );
//
//            // Set colors in gutter
//            hl.setLineMarkerRenderer(renderer);
//            // Set colors in scrollbar
//            hl.setErrorStripeMarkColor(timeColor);
//            hl.setErrorStripeTooltip(null);
//            hl.setThinErrorStripeMark(true);
//            addHighlighter(hl, data);
////        }
//    }
}

