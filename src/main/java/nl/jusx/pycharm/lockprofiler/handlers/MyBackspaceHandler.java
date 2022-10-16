package nl.jusx.pycharm.lockprofiler.handlers;

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import nl.jusx.pycharm.lockprofiler.service.ProfileHighlightService;
import org.jetbrains.annotations.NotNull;


public class MyBackspaceHandler extends BackspaceHandlerDelegate {
    @Override
    public void beforeCharDeleted(char c, @NotNull PsiFile file, @NotNull Editor editor) {
        ProfileHighlightService profileHighlightService = editor.getProject().getService(ProfileHighlightService.class);
        profileHighlightService.disposeHighlightersOverlappingAtCaretPositions(editor);
    }

    @Override
    public boolean charDeleted(char c, @NotNull PsiFile file, @NotNull Editor editor) {
        return true;
    }
}
