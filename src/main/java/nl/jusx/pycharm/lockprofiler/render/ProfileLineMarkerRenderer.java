package nl.jusx.pycharm.lockprofiler.render;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.LineMarkerRendererEx;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.editor.Editor;

import java.awt.*;

public class ProfileLineMarkerRenderer implements LineMarkerRendererEx {
    private final TextAttributesKey myAttributesKey;
    private final int myThickness;
    private final int myDepth;
    private final Position myPosition;

    private final Color myColor;


    public ProfileLineMarkerRenderer(Editor editor, @NotNull TextAttributesKey attributesKey, int thickness, int depth, @NotNull Position position) {
        myAttributesKey = attributesKey;
        myThickness = thickness;
        myDepth = depth;
        myPosition = position;

        myColor = editor.getColorsScheme().getAttributes(attributesKey).getBackgroundColor();
    }

    @Override
    public void paint(@NotNull Editor editor, @NotNull Graphics g, @NotNull Rectangle r) {
        if (myColor == null) return;

        g.setColor(myColor);
        g.fillRect(r.x, r.y, myThickness, r.height);
    }

    public @NotNull TextAttributesKey getAttributesKey() {
        return myAttributesKey;
    }

    public int getDepth() {
        return myDepth;
    }

    public int getThickness() {
        return myThickness;
    }

    @Override
    public @NotNull Position getPosition() {
        return myPosition;
    }

    public Color getColor() {
        return myColor;
    }
}
