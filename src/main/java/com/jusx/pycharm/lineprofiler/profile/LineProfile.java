package com.jusx.pycharm.lineprofiler.profile;

public class LineProfile {
    private int lineNo;
    float time;
    float hits;

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
}
