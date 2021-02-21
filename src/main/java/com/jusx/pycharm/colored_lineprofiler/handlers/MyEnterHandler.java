package com.jusx.pycharm.colored_lineprofiler.handlers;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import com.jusx.pycharm.colored_lineprofiler.service.ProfileHighlightService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MyEnterHandler implements EnterHandlerDelegate {
    @Override
    public Result preprocessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull Ref<Integer> caretOffset, @NotNull Ref<Integer> caretAdvance, @NotNull DataContext dataContext, @Nullable EditorActionHandler originalHandler) {
        ProfileHighlightService profileHighlightService = editor.getProject().getService(ProfileHighlightService.class);
        profileHighlightService.disposeHighlightersOverlappingAtCaretPositions(editor);

        return Result.Default;
    }

    @Override
    public Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext) {
        return Result.Default;
    }
}
