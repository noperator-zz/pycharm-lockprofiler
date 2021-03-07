package com.jusx.pycharm.lineprofiler.render;


import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

abstract class BaseProfileHighlighterRenderer implements CustomHighlighterRenderer {
    private static final int RESULTS_TABLE_X_MARGIN = 60;

    private final TableAlignment desiredTableAlignment;

    private final Color textColor;
    private final Font font;

    BaseProfileHighlighterRenderer(TextAttributesKey textColorKey, Font font, TableAlignment desiredTableAlignment) {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();

        textColor = scheme.getAttributes(textColorKey).getForegroundColor();
        this.font = font;
        this.desiredTableAlignment = desiredTableAlignment;
    }

    @Override
    public void paint(@NotNull Editor editor, @NotNull RangeHighlighter highlighter, @NotNull Graphics g) {
        Point resultsTableRenderAnchor = getTableRenderPoint(editor, highlighter);
        paintTableAligned(editor, highlighter, g, resultsTableRenderAnchor);
    }

    protected Point getRenderPoint(Editor editor, RangeHighlighter highlighter) {
        int trueLineNr = editor.offsetToLogicalPosition(highlighter.getStartOffset()).line;
        int lineEndOffset = editor.getDocument().getLineEndOffset(trueLineNr);
        return editor.logicalPositionToXY(editor.offsetToLogicalPosition(lineEndOffset));
    }

    protected Point getTableRenderPoint(Editor editor, RangeHighlighter highlighter) {
        return getRenderPoint(editor, highlighter);
    }


    protected void paintTableAligned(Editor editor, RangeHighlighter highlighter, Graphics g, Point renderAnchor) {
        // align table
        renderAnchor.x = desiredTableAlignment.align(renderAnchor.x);

        String text = getResultTableString(editor, highlighter);
        paintResultTableString(editor, g, renderAnchor, text);
    }

    protected int paintString(Editor editor, Graphics g, Point paintOrigin, String text) {
        g.setColor(textColor);

        g.setFont(font);

        int fontLevelFromTop = editor.getLineHeight() / 2 + g.getFont().getSize() / 2;
        int y = paintOrigin.y + fontLevelFromTop;

        int x = paintOrigin.x;

        g.drawString(text, x, y);
        return g.getFontMetrics().stringWidth(text);
    }

    protected void paintResultTableString(Editor editor, Graphics g, Point paintOrigin, String text) {
        paintOrigin = new Point(paintOrigin.x + RESULTS_TABLE_X_MARGIN, paintOrigin.y);
        paintString(editor, g, paintOrigin, text);
    }

    protected String getResultTableString(Editor editor, RangeHighlighter highlighter) {
        return "";
    }
}

