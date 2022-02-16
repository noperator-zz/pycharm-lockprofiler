package nl.jusx.pycharm.lineprofiler.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import jViridis.ColorMap;
import nl.jusx.pycharm.lineprofiler.profile.LineProfile;
import nl.jusx.pycharm.lineprofiler.settings.SettingsState;

import java.util.HashSet;
import java.util.Set;


/**
 * Services that provides the Line Profiler plugin with a map between line profile results and colors
 */
@Service
public final class ColorMapService {
    private static final int AMOUNT_OF_COLORS = 40;

    private Set<ColorMapOption> loadedColorMaps = new HashSet<>();


    /**
     * Loads all colors that can be used as highlight background color
     */
    private void loadBackgroundColors(ColorMapOption colorMapOption) {
        ColorMap cm = ColorMap.getInstance(colorMapOption.getIdentifier());
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();

        for (int i = 0; i < AMOUNT_OF_COLORS; i++) {
            TextAttributes ta = new TextAttributes();

            float colorFrac = ((float) i) / ((float) AMOUNT_OF_COLORS - 1);
            ta.setBackgroundColor(cm.getColor(colorFrac));

            // Register TextAttributes on global editor scheme
            TextAttributesKey key = TextAttributesKey.createTextAttributesKey(colorMapOption.getIdentifier() + i);
            scheme.setAttributes(key, ta);
        }

        loadedColorMaps.add(colorMapOption);
    }

    public TextAttributesKey getTimeFractionTextAttributesKey(LineProfile line, long timeDenominator) {
        ColorMapOption colorMapOption = SettingsState.getInstance().getColorMap();
        if (!loadedColorMaps.contains(colorMapOption)) {
            loadBackgroundColors(colorMapOption);
        }

        float timeFraction = line.getTimeFraction(timeDenominator);

        int colorIndex = (int) (timeFraction * (float) (AMOUNT_OF_COLORS - 1));

        return TextAttributesKey.createTextAttributesKey(colorMapOption.getIdentifier() + colorIndex);
    }
}
