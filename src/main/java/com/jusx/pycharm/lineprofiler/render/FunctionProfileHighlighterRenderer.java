package com.jusx.pycharm.lineprofiler.render;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.RangeHighlighter;

import java.awt.*;

/**
 * Renderer that renders meta data about a function profile and it's visualizations
 * This renderer is typically used on the `@profile` line above a python function
 */
public class FunctionProfileHighlighterRenderer extends BaseProfileHighlighterRenderer {
    public FunctionProfileHighlighterRenderer(TextAttributesKey textColorKey, Font font, int resultsXAlignment) {
        super(textColorKey, font, resultsXAlignment);
    }

    @Override
    protected String getResultTableString(RangeHighlighter highlighter) {
        return String.format("%6s%15s%15s%15s",
                "% Time", "Hits", "Time", "Time / Hit");
    }
}