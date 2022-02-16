package nl.jusx.pycharm.lineprofiler.service;

import jViridis.ColorMap;

public enum ColorMapOption {
    VIRIDIS(ColorMap.VIRIDIS),
    INFERNO(ColorMap.INFERNO),
    MAGMA(ColorMap.MAGMA),
    PLASMA(ColorMap.PLASMA);

    private final String identifier;

    ColorMapOption(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
    }
}
