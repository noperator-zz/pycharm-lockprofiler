package nl.jusx.pycharm.lineprofiler.profile;

public class LineProfile implements LineProvider {
    private final int lineNo;
    float time;
    int hits;

    LineProfile(ProfileSchema.Function.Line lineSchema) {
        lineNo = lineSchema.lineNo;
        time = lineSchema.time;
        hits = lineSchema.hits;
    }

    public int getLineNrFromZero() {
        return lineNo - 1;
    }

    public float getTime() {
        return time;
    }

    public float getTimeFraction(float timeDenominator) {
        return time / timeDenominator;
    }

    public int getHits() {
        return hits;
    }
}
