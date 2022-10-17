package nl.jusx.pycharm.lockprofiler.render;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import nl.jusx.pycharm.lockprofiler.profile.ProfileSchema;
import nl.jusx.pycharm.lockprofiler.service.ColorMapService;
import nl.jusx.pycharm.lockprofiler.service.ProfileHighlightService;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import static nl.jusx.pycharm.lockprofiler.render.InlayRendererUtils.*;


/**
 * Inlay renderer that renders the results table entries after lines.
 *
 * Inspired by {@link com.intellij.xdebugger.impl.inline.InlineDebugRenderer}
 */
public class LineProfileInlayRenderer implements EditorCustomElementRenderer {
    public static final int RESULT_TABLE_STRING_MARGIN_BLOCKS = 3;

    private final ProfileSchema schema;
//    private final Map<Integer, ProfileSchema.LockStats> lock_stats;
//    private final long timeDenominator;
    private final int lineNo;
//    private final int numLocks;
//    private final long total_acquire_time;
//    private final long total_hold_time;
//    private final long total_hits;
    private final String text;
    private final TableAlignment tableAlignment;
    private final int margin;

    public LineProfileInlayRenderer(
            ProfileSchema schema,
            int lineNo,
            Map<Long, ProfileSchema.LockStats> lock_stats,
//          long timeDenominator,
            TableAlignment tableAlignment,
            int margin) {

        this.schema = schema;
        //        this.lock_stats = lock_stats;
//        this.timeDenominator = timeDenominator;
        this.lineNo = lineNo;
//        this.numLocks = lock_stats.size();
//        this.total_hits = lock_stats.values().stream().mapToLong(s -> s.hits).sum();
//        this.total_acquire_time = lock_stats.values().stream().mapToLong(s -> s.total_acquire_time).sum();
//        this.total_hold_time = lock_stats.values().stream().mapToLong(s -> s.total_hold_time).sum();
        List<String> texts = lock_stats
                .entrySet()
                .stream()
                .sorted(Comparator.comparingLong(e->e.getValue().total_acquire_time))
                .map(e -> {
                    return String.format("%s %d %d %s %s",
                        schema.getLockName(e.getKey()),
                        e.getValue().hits,
                        e.getValue().acquires,
                        ProfileSchema.formatTime(e.getValue().total_acquire_time),
                        ProfileSchema.formatTime(e.getValue().total_hold_time)
                    );})
                .collect(Collectors.toList());

        this.text = String.join("|", texts);
        this.tableAlignment = tableAlignment;
        this.margin = margin;
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        // TODO this doesn't seem to take into account that the inlay may have blank space on the left due to tableAlignment
        FontMetrics metrics = getFontMetrics(inlay.getEditor());

        return RESULT_TABLE_STRING_MARGIN_BLOCKS * margin +
                // 53 ' ' because of the results table string format (6 + 15 + 15 + 17)
                metrics.charWidth(' ') * text.length();
    }

    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
        Editor editor = inlay.getEditor();
        paintTableAligned(editor, g, targetRegion.getLocation());
    }

    private void paintTableAligned(@NotNull Editor editor, @NotNull Graphics g, @NotNull Point renderAnchor) {
        // Table begins rendering x margins to the right
        renderAnchor.x = tableAlignment.align(
                editor, renderAnchor.x + RESULT_TABLE_STRING_MARGIN_BLOCKS * margin,
                String.valueOf(lineNo - 1));
//        System.out.printf("%d %d\n", renderAnchor.y, renderAnchor.x);
        // Color begins rendering 2 margins to the left of that
        Point colorAnchor = renderAnchor.getLocation();
        colorAnchor.x -= 2 * margin;

        paintColorbar(editor, g, colorAnchor);
        paintResultTableString(editor, g, renderAnchor);
    }

    /**
     * Paints the color block next to a line, visualizing the timefraction
     * @param editor editor to draw for
     * @param g graphic to draw in
     * @param renderAnchor anchor for results rendering
     */
    private void paintColorbar(@NotNull Editor editor, @NotNull Graphics g, @NotNull Point renderAnchor) {
        ColorMapService colorMapService = ApplicationManager.getApplication().getService(ColorMapService.class);

        float timeFraction = 1;//lock_stats.getTimeFraction(timeDenominator);
        TextAttributesKey key = colorMapService.getTimeFractionTextAttributesKey(timeFraction);

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

        g.drawString(getResultTableString(x), x, y);
    }

    private String getResultTableString(int x) {
        return text;
//        return String.format("%5d locks:%-3d hits:%-5d wait:%-10s hold:%-10s",
//                x,
//                numLocks,
//                total_hits,
//                ProfileSchema.formatTime(total_acquire_time),
//                ProfileSchema.formatTime(total_hold_time)
//        );
    }
}
