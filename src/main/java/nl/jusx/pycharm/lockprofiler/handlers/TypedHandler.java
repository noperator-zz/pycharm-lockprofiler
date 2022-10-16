package nl.jusx.pycharm.lockprofiler.handlers;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import nl.jusx.pycharm.lockprofiler.service.ProfileHighlightService;
import org.jetbrains.annotations.NotNull;

public class TypedHandler extends TypedHandlerDelegate {

    @Override
    public @NotNull Result beforeCharTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @NotNull FileType fileType) {
        ProfileHighlightService profileHighlightService = project.getService(ProfileHighlightService.class);
        profileHighlightService.disposeHighlightersOverlappingAtCaretPositions(editor);

        return super.beforeCharTyped(c, project, editor, file, fileType);
    }
}
