package com.jusx.pycharm.colored_lineprofiler.handlers;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.jusx.pycharm.colored_lineprofiler.service.ProfileHighlightService;
import org.jetbrains.annotations.NotNull;

public class TypedHandler extends TypedHandlerDelegate {

    @Override
    public @NotNull Result beforeCharTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @NotNull FileType fileType) {
        ProfileHighlightService profileHighlightService = project.getService(ProfileHighlightService.class);
        profileHighlightService.disposeHighlightersOverlappingAtCaretPositions(editor);

        return super.beforeCharTyped(c, project, editor, file, fileType);
    }
}
