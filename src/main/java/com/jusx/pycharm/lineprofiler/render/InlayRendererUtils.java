package com.jusx.pycharm.lineprofiler.render;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.util.ui.UIUtil;
import com.intellij.xdebugger.ui.DebuggerColors;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class InlayRendererUtils {
    private static final TextAttributesKey ATTRIBUTES_KEY = DebuggerColors.INLINED_VALUES;

    public static Font getFont(@NotNull Editor editor, int style) {
        EditorColorsScheme colorsScheme = editor.getColorsScheme();
        return UIUtil.getFontWithFallback(colorsScheme.getEditorFontName(), style, colorsScheme.getEditorFontSize());
    }

    public static FontMetrics getFontMetrics(@NotNull Editor editor) {
        Font font = editor.getColorsScheme().getFont(EditorFontType.PLAIN);
        return FontInfo.getFontMetrics(font, FontInfo.getFontRenderContext(editor.getContentComponent()));
    }

    public static int getMargin(FontMetrics metrics) {
        return metrics.charWidth(' ') * 3;
    }

    public static TextAttributes getAttributes(@NotNull Editor editor) {
        return editor.getColorsScheme().getAttributes(ATTRIBUTES_KEY);
    }
}
