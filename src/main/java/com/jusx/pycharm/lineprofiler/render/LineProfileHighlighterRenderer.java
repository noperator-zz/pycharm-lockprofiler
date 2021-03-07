package com.jusx.pycharm.lineprofiler.render;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.jusx.pycharm.lineprofiler.profile.LineProfile;
import com.jusx.pycharm.lineprofiler.service.ColorMapService;
import com.jusx.pycharm.lineprofiler.service.ProfileHighlightService;

import java.awt.*;

/**
 * Renderer which renders line profile results behind code in the editor
 */
public class LineProfileHighlighterRenderer extends BaseProfileHighlighterRenderer {
    private static final Logger logger = Logger.getInstance(LineProfileHighlighterRenderer.class.getName());

    private static final int RESULTS_COLORBAR_X_MARGIN = 20;
    private static final int RESULTS_COLORBAR_X_WIDTH = 20;

    private final float timeDenominator;

    public LineProfileHighlighterRenderer(TextAttributesKey textColorKey, Font font,
                                          TableAlignment desiredTableAlignment, float timeDenominator) {
        super(textColorKey, font, desiredTableAlignment);
        this.timeDenominator = timeDenominator;
    }

    @Override
    protected void paintTableAligned(Editor editor, RangeHighlighter highlighter, Graphics g, Point renderAnchor) {
        super.paintTableAligned(editor, highlighter, g, renderAnchor);
        paintColorbar(editor, highlighter, g, renderAnchor);
    }

    /**
     * Paints the color block next to a line, visualizing the timefraction
     * @param editor editor to draw for
     * @param highlighter highlighter to paint colorbar for, is used to look up the line profile
     * @param g graphic to draw in
     * @param renderOrigin anchor for results rendering
     */
    private void paintColorbar(Editor editor, RangeHighlighter highlighter, Graphics g, Point renderOrigin) {
        ProfileHighlightService profileHighlightService = editor.getProject().getService(ProfileHighlightService.class);

        // Get LineProfile based on the highlighter
        LineProfile lineProfile = profileHighlightService.getLineProfile(highlighter);
        assert lineProfile != null;

        ColorMapService colorMapService = ServiceManager.getService(ColorMapService.class);
        TextAttributesKey key = colorMapService.getTimeFractionTextAttributesKey(lineProfile, timeDenominator);

        Color color = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(key).getBackgroundColor();

        g.setColor(color);
        g.fillRect(renderOrigin.x + RESULTS_COLORBAR_X_MARGIN, renderOrigin.y,
                RESULTS_COLORBAR_X_WIDTH, editor.getLineHeight());
//        g.fillRect(point.x - JBUIScale.scale(1), point.y, JBUIScale.scale(2), editor.getLineHeight());
    }

    @Override
    protected String getResultTableString(Editor editor, RangeHighlighter highlighter) {
        ProfileHighlightService profileHighlightService = editor.getProject().getService(ProfileHighlightService.class);

        // Get LineProfile based on the highlighter
        LineProfile lineProfile = profileHighlightService.getLineProfile(highlighter);
        assert lineProfile != null;

        return String.format("%6.1f%15d%15.0f%15.1f",
                lineProfile.getTimeFraction(timeDenominator) * 100,
                lineProfile.getHits(),
                lineProfile.getTime(),
                lineProfile.getTime() / lineProfile.getHits());
    }
}
