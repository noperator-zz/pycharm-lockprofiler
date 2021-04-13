package nl.jusx.pycharm.lineprofiler.render;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import nl.jusx.pycharm.lineprofiler.profile.LineProfile;
import nl.jusx.pycharm.lineprofiler.service.ColorMapService;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static nl.jusx.pycharm.lineprofiler.render.InlayRendererUtils.*;


/**
 * Inlay renderer that renders the results table entries after lines.
 *
 * Inspired by {@link com.intellij.xdebugger.impl.inline.InlineDebugRenderer}
 */
public class LineProfileInlayRenderer implements EditorCustomElementRenderer {
    private static final int MAX_TABLE_ALIGNMENT_IN_CHARS = 75;
    private static final int RESULT_TABLE_STRING_MARGIN_BLOCKS = 3;

    private final LineProfile lineProfile;
    private final float timeDenominator;
    private final TableAlignment tableAlignment;
    private final int margin;

    public LineProfileInlayRenderer(LineProfile lineProfile,
                                    float timeDenominator,
                                    TableAlignment tableAlignment,
                                    int margin) {
        this.lineProfile = lineProfile;
        this.timeDenominator = timeDenominator;
        this.tableAlignment = tableAlignment;
        this.margin = margin;
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        FontMetrics metrics = getFontMetrics(inlay.getEditor());

        return RESULT_TABLE_STRING_MARGIN_BLOCKS * margin +
                // 51 ' ' because of the results table string format (6 + 15 + 15 + 15)
                metrics.charWidth(' ') * 51;
    }

    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
        Editor editor = inlay.getEditor();
        paintTableAligned(editor, g, targetRegion.getLocation());
    }

    private void paintTableAligned(@NotNull Editor editor, @NotNull Graphics g, @NotNull Point renderAnchor) {
        // align table
        renderAnchor.x += 3 * margin;
        if (renderAnchor.x < getFontMetrics(editor).charWidth(' ') * MAX_TABLE_ALIGNMENT_IN_CHARS) {
            renderAnchor.x = tableAlignment.align(renderAnchor.x, String.valueOf(lineProfile.getLineNrFromZero()));
        }

        Point colorAnchor = renderAnchor.getLocation();

        colorAnchor.x -= 2 * margin;
        paintColorbar(editor, g, colorAnchor);

        paintResultTableString(editor, g, renderAnchor);
    }

    /**
     * Paints the color block next to a line, visualizing the timefraction
     * @param editor editor to draw for
     * @param highlighter highlighter to paint colorbar for, is used to look up the line profile
     * @param g graphic to draw in
     * @param renderOrigin anchor for results rendering
     */
    private void paintColorbar(@NotNull Editor editor, @NotNull Graphics g, @NotNull Point renderAnchor) {
        ColorMapService colorMapService = ServiceManager.getService(ColorMapService.class);
        TextAttributesKey key = colorMapService.getTimeFractionTextAttributesKey(lineProfile, timeDenominator);

        Color color = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(key).getBackgroundColor();

        g.setColor(color);
        g.fillRect(renderAnchor.x, renderAnchor.y,
                margin, editor.getLineHeight());
    }

    private void paintResultTableString(@NotNull Editor editor, @NotNull Graphics g, @NotNull Point paintOrigin) {
        Font font = getFont(editor, Font.ITALIC);
        g.setFont(font);

        Color textColor = getAttributes(editor).getForegroundColor();
        g.setColor(textColor);

        int x = paintOrigin.x;
        int y = paintOrigin.y + editor.getAscent();

        g.drawString(getResultTableString(), x, y);
    }

    private String getResultTableString() {

        return String.format("%6.1f%15d%15.0f%15.1f",
                lineProfile.getTimeFraction(timeDenominator) * 100,
                lineProfile.getHits(),
                lineProfile.getTime(),
                lineProfile.getTime() / lineProfile.getHits());
    }
}
