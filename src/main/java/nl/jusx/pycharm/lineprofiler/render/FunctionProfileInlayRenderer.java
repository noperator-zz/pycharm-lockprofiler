package nl.jusx.pycharm.lineprofiler.render;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import jViridis.ColorMap;
import nl.jusx.pycharm.lineprofiler.profile.FunctionProfile;
import nl.jusx.pycharm.lineprofiler.profile.Profile;
import nl.jusx.pycharm.lineprofiler.profile.ProfileSchema;
import nl.jusx.pycharm.lineprofiler.service.ColorMapService;
import nl.jusx.pycharm.lineprofiler.settings.SettingsState;
import nl.jusx.pycharm.lineprofiler.render.LineProfileInlayRenderer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static nl.jusx.pycharm.lineprofiler.render.InlayRendererUtils.getAttributes;
import static nl.jusx.pycharm.lineprofiler.render.InlayRendererUtils.getFont;

public class FunctionProfileInlayRenderer implements EditorCustomElementRenderer {
    private static final int COLORMAP_DISCRETIZATION = 10;
    private static final int COLORMAP_GRADIENT_WIDTH_IN_CHARS = 10;

    private final FunctionProfile functionProfile;
    private final TableAlignment tableAlignment;
    private final int margin;
    private final long fileTotalTime;

    public FunctionProfileInlayRenderer(FunctionProfile functionProfile,
                                        TableAlignment tableAlignment,
                                        int margin,
                                        long fileTotalTime) {
        this.functionProfile = functionProfile;
        this.tableAlignment = tableAlignment;
        this.margin = margin;
        this.fileTotalTime = fileTotalTime;
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        // TODO this doesn't seem to take into account that the inlay may have blank space on the left due to tableAlignment
        return 100;
    }

//    @Override
//    public int calcHeightInPixels(@NotNull Inlay inlay) {
//        return inlay.getEditor().getLineHeight() * 3;
//    }

    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
        Editor editor = inlay.getEditor();

        Point anchor = targetRegion.getLocation();

        Font font = getFont(editor, Font.PLAIN);
        g.setFont(font);
        paintTimeInFunction(editor, g, anchor);
//        paintColormapWithLegend(editor, g, targetRegion);

        anchor.x += 1 * margin;

//        ColorMapService colorMapService = ApplicationManager.getApplication().getService(ColorMapService.class);
//
//        double timeFraction = (double) functionProfile.getTotalTime() / fileTotalTime;
//        TextAttributesKey key = colorMapService.getTimeFractionTextAttributesKey(timeFraction);
//
//        Color color = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(key).getBackgroundColor();
//
//        g.setColor(color);
//        g.fillRect(anchor.x, anchor.y, margin, editor.getLineHeight());// * functionProfile.getNumLines());

        anchor.x += 2 * margin;

        font = font.deriveFont(Font.BOLD);
        g.setFont(font);
        paintTableHeader(editor, g, anchor);
    }

    private void paintTimeInFunction(@NotNull Editor editor, @NotNull Graphics g, @NotNull Point renderAnchor) {
        Color textColor = getAttributes(editor).getForegroundColor();
        g.setColor(textColor);


        String text = String.format("Time in function: %s (%.1f %%)",
                ProfileSchema.formatTime(functionProfile.getTotalTime()),
                (double) functionProfile.getTotalTime() * 100 / fileTotalTime
        );
        g.drawString(text, renderAnchor.x, renderAnchor.y + editor.getAscent());
        renderAnchor.x += g.getFontMetrics().stringWidth(text);

        renderAnchor.x = tableAlignment.align(editor, renderAnchor.x + 3 * margin,
                "timeInFunction" + functionProfile.getFile() + String.valueOf(functionProfile.getLineNrFromZero()));
        renderAnchor.x -= 3 * margin;
    }
//
//    private void paintColormapWithLegend(@NotNull Editor editor, @NotNull Graphics g, @NotNull Rectangle targetRegion) {
//        Color textColor = getAttributes(editor).getForegroundColor();
//        g.setColor(textColor);
//
//        FontMetrics metrics = g.getFontMetrics();
//
//        Point p = targetRegion.getLocation();
//        int currentX = p.x;
//        int currentY = p.y + editor.getLineHeight();
//
//        String text = "Colormap '%Time': ";
//        g.drawString(text, currentX, currentY + editor.getAscent());
//        currentX += metrics.stringWidth(text);
//
//        currentX = tableAlignment.align(editor, currentX, "colormap");
//
//        text = "0% ";
//        g.drawString(text, currentX, currentY + editor.getAscent());
//        currentX += metrics.stringWidth(text);
//
//        int colormapWidth = metrics.charWidth(' ') * COLORMAP_GRADIENT_WIDTH_IN_CHARS;
//        paintColormap(g,
//                currentX,
//                currentY,
//                colormapWidth,
//                editor.getLineHeight());
//        currentX += colormapWidth;
//
//        g.setColor(textColor);
//        text = " 100%";
//        g.drawString(text, currentX, currentY + editor.getAscent());
//    }
//
//    private void paintColormap(Graphics g, int x, int y, int width, int height) {
//        Graphics2D g2 = (Graphics2D) g;
//
//        ColorMap cm = ColorMap.getInstance(SettingsState.getInstance().getColorMap().getIdentifier());
//        Color startColor, endColor;
//
//        startColor = cm.getColor(0f);
//
//        for (int i = 1; i <= COLORMAP_DISCRETIZATION; i++) {
//            float gradientStartFrac = ((float) (i - 1)) / COLORMAP_DISCRETIZATION;
//            float gradientEndFrac = ((float) i) / COLORMAP_DISCRETIZATION;
//
//            int gradientStartX = x + (int) (gradientStartFrac * width);
//            int gradientEndX = x + (int) (gradientEndFrac * width);
//
//            endColor = cm.getColor(gradientEndFrac);
//
//            GradientPaint gradient = new GradientPaint(
//                    gradientStartX, y, startColor,
//                    gradientEndX, y, endColor);
//            g2.setPaint(gradient);
//            g2.fillRect(gradientStartX, y, gradientEndX - gradientStartX, height);
//
//            startColor = endColor;
//        }
//    }

    private void paintTableHeader(@NotNull Editor editor, @NotNull Graphics g, @NotNull Point renderAnchor) {
        Color textColor = getAttributes(editor).getForegroundColor();
        g.setColor(textColor);

        g.drawString(
                String.format(
                        "%6s%15s%15s%17s",
                        "% Time",
                        "Hits",
                        "Time",
                        "Time / Hit"
//                        String.format("Time [%s]", profile.getUnitShort()),
//                        String.format("Time / Hit [%s]", profile.getUnitShort())
                        ),
                renderAnchor.x, renderAnchor.y + editor.getAscent());
    }
}
