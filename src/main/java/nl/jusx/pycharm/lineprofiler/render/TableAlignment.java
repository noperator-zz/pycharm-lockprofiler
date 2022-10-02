package nl.jusx.pycharm.lineprofiler.render;


import com.intellij.openapi.project.Project;
import nl.jusx.pycharm.lineprofiler.settings.SettingsState;
import com.intellij.openapi.editor.Editor;

import java.util.HashMap;
import java.util.Map;

import static nl.jusx.pycharm.lineprofiler.render.InlayRendererUtils.getFontMetrics;

/**
 * Object that is shared by renderers
 *
 * With this object a shared alignment can be decided and kept in memory
 */
public class TableAlignment {
    private final Map<String, Integer> alignment = new HashMap<>();

    /**
     * Take value into account for alignments
     *
     * A key is used to so that previous values of an alignment can be replaced
     *
     * @param withX value to take into consideration for alignment
     * @param key key to which value belongs
     * @return new alignment value (taking into account the just added value)
     */
    public int align(Editor editor, int withX, String key) {
        if (withX >= getFontMetrics(editor).charWidth(' ') * SettingsState.getInstance().getTableAlignmentMaxColumns()) {
            // Don't update alignemts past some maximum.
            // Any line over the maximum will be misaligned relative to others using the same TableAlignment
            return withX;
        }
        alignment.put(key, withX);
        return alignment.values().stream().max(Integer::compare).orElseThrow();
    }

//    public int getX() {
//        return alignment.values().stream().max(Integer::compare).orElse(0);
//    }
}
