package com.jusx.pycharm.lineprofiler.render;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.jusx.pycharm.lineprofiler.profile.FunctionProfile;
import com.jusx.pycharm.lineprofiler.profile.Profile;
import com.jusx.pycharm.lineprofiler.service.TimeFractionCalculation;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Renderer that renders meta data about a function profile and it's visualizations
 * This renderer is typically used on the `@profile` line above a python function
 */
public class FunctionProfileHighlighterRenderer extends BaseProfileHighlighterRenderer {
    private static final int COLORMAP_LEFT_MARGIN = 20;

    private final FunctionProfile functionProfile;
    private final Profile profile;
    private final TimeFractionCalculation timeFractionCalculation;

    private int colormapWidth = 0;


    public FunctionProfileHighlighterRenderer(
            TextAttributesKey textColorKey, Font font,
            TableAlignment desiredTableAlignment,
            FunctionProfile functionProfile, Profile profile,
            TimeFractionCalculation timeFractionCalculation) {
        super(textColorKey, font, desiredTableAlignment);

        this.functionProfile = functionProfile;
        this.profile = profile;
        this.timeFractionCalculation = timeFractionCalculation;
    }

    @Override
    public void paint(@NotNull Editor editor, @NotNull RangeHighlighter highlighter, @NotNull Graphics g) {
        paintFunctionProfileMetaData(editor, highlighter, g);
        Point anchor = getRenderPoint(editor, highlighter);
        paintColormap(editor, g, anchor);
        super.paint(editor, highlighter, g);
    }

    @Override
    protected Point getTableRenderPoint(Editor editor, RangeHighlighter highlighter) {
        // Add space needed for colormap to the table render point so that the results table
        // doesnt accidentally overlap with the colormap
        Point p = getRenderPoint(editor, highlighter);
        p.x += + COLORMAP_LEFT_MARGIN + colormapWidth;
        return p;
    }

    private void paintFunctionProfileMetaData(Editor editor, RangeHighlighter highlighter, Graphics g) {
        LogicalPosition functionIndentation = editor.offsetToLogicalPosition(highlighter.getStartOffset());
        Point metadataAnchor = editor.logicalPositionToXY(functionIndentation);
        metadataAnchor.y = metadataAnchor.y - editor.getLineHeight();

        paintString(editor, g, metadataAnchor,
                String.format("FT: %.0f - TPT: %.0f [%.6f s]   |   FT = Function Time - TPT = Total Profile Time",
                        functionProfile.getTotalTime(),
                        profile.getTotalTime(),
                        profile.getUnit()));
    }

    private void paintColormap(Editor editor, Graphics g, Point metadataAnchor) {
        int width = 0;
        width += paintString(editor, g, metadataAnchor, "0 ");
        metadataAnchor.x = metadataAnchor.x + width;

        width += paintString(editor, g, metadataAnchor, " 100");
        colormapWidth = width;
    }

    @Override
    protected String getResultTableString(Editor editor, RangeHighlighter highlighter) {
        String percTime = "";
        if (timeFractionCalculation == TimeFractionCalculation.PROFILE_TOTAL) {
            percTime = "% TPT";
        } else if (timeFractionCalculation == TimeFractionCalculation.FUNCTION_TOTAL) {
            percTime = "% FT";
        }
        return String.format("%6s%15s%15s%15s",
                percTime, "Hits", "Time", "Time / Hit");
    }
}