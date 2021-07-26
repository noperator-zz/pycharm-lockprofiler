package nl.jusx.pycharm.lineprofiler.render;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.markup.TextAttributes;
import nl.jusx.pycharm.lineprofiler.profile.FunctionProfile;
import nl.jusx.pycharm.lineprofiler.profile.Profile;
import jViridis.ColorMap;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static nl.jusx.pycharm.lineprofiler.render.InlayRendererUtils.getAttributes;
import static nl.jusx.pycharm.lineprofiler.render.InlayRendererUtils.getFont;

public class FunctionProfileInlayRenderer implements EditorCustomElementRenderer {
    private static final int COLORMAP_DISCRETIZATION = 10;
    private static final int COLORMAP_GRADIENT_WIDTH_IN_CHARS = 10;

    private final FunctionProfile functionProfile;
    private final Profile profile;
    private final TableAlignment tableAlignment;

    public FunctionProfileInlayRenderer(FunctionProfile functionProfile,
                                        Profile profile,
                                        TableAlignment tableAlignment) {
        this.functionProfile = functionProfile;
        this.profile = profile;
        this.tableAlignment = tableAlignment;
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        return 100;
    }

    @Override
    public int calcHeightInPixels(@NotNull Inlay inlay) {
        return inlay.getEditor().getLineHeight() * 3;
    }

    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
        Editor editor = inlay.getEditor();

        Font font = getFont(editor, Font.PLAIN);
        g.setFont(font);
        paintTimeInFunction(editor, g, targetRegion);
        paintColormapWithLegend(editor, g, targetRegion);

        font = font.deriveFont(Font.BOLD);
        g.setFont(font);
        paintTableHeader(editor, g, targetRegion);
    }

    private void paintTimeInFunction(@NotNull Editor editor, @NotNull Graphics g, @NotNull Rectangle targetRegion) {
        Color textColor = getAttributes(editor).getForegroundColor();
        g.setColor(textColor);

        Point p = targetRegion.getLocation();
        int currentX = p.x;
        String text = "Time in function: ";
        g.drawString(text, currentX, p.y + editor.getAscent());
        currentX += g.getFontMetrics().stringWidth(text);

        currentX = tableAlignment.align(currentX, "timeInFunction");

        g.drawString(
                String.format("%.0f %s",
                        functionProfile.getTotalTime(),
                        profile.getUnitLong()),
                currentX,
                p.y + editor.getAscent());
    }

    private void paintColormapWithLegend(@NotNull Editor editor, @NotNull Graphics g, @NotNull Rectangle targetRegion) {
        Color textColor = getAttributes(editor).getForegroundColor();
        g.setColor(textColor);

        FontMetrics metrics = g.getFontMetrics();

        Point p = targetRegion.getLocation();
        int currentX = p.x;
        int currentY = p.y + editor.getLineHeight();

        String text = "Colormap '%Time': ";
        g.drawString(text, currentX, currentY + editor.getAscent());
        currentX += metrics.stringWidth(text);

        currentX = tableAlignment.align(currentX, "colormap");

        text = "0% ";
        g.drawString(text, currentX, currentY + editor.getAscent());
        currentX += metrics.stringWidth(text);

        int colormapWidth = metrics.charWidth(' ') * COLORMAP_GRADIENT_WIDTH_IN_CHARS;
        paintColormap(g,
                currentX,
                currentY,
                colormapWidth,
                editor.getLineHeight());
        currentX += colormapWidth;

        g.setColor(textColor);
        text = " 100%";
        g.drawString(text, currentX, currentY + editor.getAscent());
    }

    private void paintColormap(Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g;

        ColorMap cm = ColorMap.getInstance(ColorMap.VIRIDIS);
        Color startColor, endColor;

        startColor = cm.getColor(0f);

        for (int i = 1; i <= COLORMAP_DISCRETIZATION; i++) {
            float gradientStartFrac = ((float) (i - 1)) / COLORMAP_DISCRETIZATION;
            float gradientEndFrac = ((float) i) / COLORMAP_DISCRETIZATION;

            int gradientStartX = x + (int) (gradientStartFrac * width);
            int gradientEndX = x + (int) (gradientEndFrac * width);

            endColor = cm.getColor(gradientEndFrac);

            GradientPaint gradient = new GradientPaint(
                    gradientStartX, y, startColor,
                    gradientEndX, y, endColor);
            g2.setPaint(gradient);
            g2.fillRect(gradientStartX, y, gradientEndX - gradientStartX, height);

            startColor = endColor;
        }
    }

    private void paintTableHeader(@NotNull Editor editor, @NotNull Graphics g, @NotNull Rectangle targetRegion) {
        Color textColor = getAttributes(editor).getForegroundColor();
        g.setColor(textColor);

        Point tableHeaderAnchor = targetRegion.getLocation();
        g.drawString(
                String.format(
                        "%6s%15s%15s%17s",
                        "% Time",
                        "Hits",
                        String.format("Time [%s]", profile.getUnitShort()),
                        String.format("Time / Hit [%s]", profile.getUnitShort())),
                tableAlignment.getX(),
                tableHeaderAnchor.y + 2 * editor.getLineHeight() + editor.getAscent());
    }
}
