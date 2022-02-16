package nl.jusx.pycharm.lineprofiler.profile;

public class LineProfile implements LineProvider {
    private final int lineNo;
    long time;
    long hits;

    LineProfile(ProfileSchema.Function.Line lineSchema) {
        lineNo = lineSchema.lineNo;
        time = lineSchema.time;
        hits = lineSchema.hits;
    }

    public int getLineNrFromZero() {
        return lineNo - 1;
    }

    public long getTime() {
        return time;
    }

    public float getTimeFraction(long timeDenominator) {
        return time / (float) timeDenominator;
    }

    public long getHits() {
        return hits;
    }
}
