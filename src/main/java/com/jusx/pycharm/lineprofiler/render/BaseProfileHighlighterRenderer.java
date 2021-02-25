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

    private int resultsXAlignment;

    private final Color textColor;
    private final Font font;

    BaseProfileHighlighterRenderer(TextAttributesKey textColorKey, Font font, int resultsXAlignment) {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();

        textColor = scheme.getAttributes(textColorKey).getForegroundColor();
        this.font = font;
        this.resultsXAlignment = resultsXAlignment;
    }

    @Override
    public void paint(@NotNull Editor editor, @NotNull RangeHighlighter highlighter, @NotNull Graphics g) {
        Point resultsRenderAnchor = getAlignedRenderPoint(editor, highlighter, resultsXAlignment);
        paintAligned(editor, highlighter, g, resultsRenderAnchor);
    }

    /**
     * Returns point at which results can be rendered for a line
     *
     * The point's y-coordinate is at the top of the line
     * The point's x-coordinate marks the coordinate from which we can start rendering results.
     *      It is guaranteed that no existing text will overlap with renders after this point.
     */
    private static Point getAlignedRenderPoint(Editor editor, RangeHighlighter highlighter, int minX) {
        // When creating a LineHighlighter, getStartOffset is the same as getEndOffset
        // Both point to the start of the line, so with those functions it's not possible to get
        // the LogicalPosition of the last character of the line, which we in fact want to have because we need it
        // to determine potential alignment breaks.
        // We'll use the getStartOffset to calculate the line number and request the endoffset with that line number
        // Also: we cannot use the line nr from the profile, because the user may have moved lines in the mean time,
        // which results that the line numbers in the profile do not correspond to the current line number of the
        // highlighters
        int trueLineNr = editor.offsetToLogicalPosition(highlighter.getStartOffset()).line;
        int lineEndOffset = editor.getDocument().getLineEndOffset(trueLineNr);
        Point point = editor.logicalPositionToXY(editor.offsetToLogicalPosition(lineEndOffset));
        // Break alignment when text is too long
        point.x = Math.max(point.x, minX);
        return point;
    }

    protected void paintAligned(Editor editor, RangeHighlighter highlighter, Graphics g, Point renderAnchor) {
        paintResultTableString(editor, g, highlighter, renderAnchor);
    }

    private void paintResultTableString(Editor editor, Graphics g, RangeHighlighter highlighter, Point renderOrigin) {
        g.setColor(textColor);

        g.setFont(font);

        int fontLevelFromTop = editor.getLineHeight() / 2 + g.getFont().getSize() / 2;
        int y = renderOrigin.y + fontLevelFromTop;
        // Add margins to x coordinate
        int x = renderOrigin.x + RESULTS_TABLE_X_MARGIN;

        g.drawString(
                getResultTableString(highlighter),
                x, y);
    }

    protected String getResultTableString(RangeHighlighter highlighter) {
        return "";
    }
}

