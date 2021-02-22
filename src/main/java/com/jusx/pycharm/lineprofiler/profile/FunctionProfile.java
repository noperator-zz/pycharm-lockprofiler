package com.jusx.pycharm.lineprofiler.profile;

import java.util.ArrayList;
import java.util.List;


public class FunctionProfile {
    List<LineProfile> lineProfiles = new ArrayList<>();
    String file;
    int lineNo;
    String functionName;

    float totalTime;
    float maxLineTime;

    FunctionProfile(ProfileSchema.Function fnSchema) {
        file = fnSchema.file;
        lineNo = fnSchema.lineNo;
        functionName = fnSchema.functionName;

        for (ProfileSchema.Function.Line lineSchema : fnSchema.profiledLines) {
            LineProfile line = new LineProfile(lineSchema);

            lineProfiles.add(line);
            totalTime += line.time;

            maxLineTime = Math.max(maxLineTime, line.time);
        }
    }

    public List<LineProfile> getProfiledLines() {
        return lineProfiles;
    }

    public String getFile() {
        return file;
    }

    public String getFunctionName() {
        return functionName;
    }

    public float getTotalTime() {
        return totalTime;
    }

    public float getMaxLineTime() {
        return maxLineTime;
    }

    public int getLineNo() {
        return lineNo;
    }
}
