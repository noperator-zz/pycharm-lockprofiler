package com.jusx.pycharm.lineprofiler.profile;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class FunctionProfile implements LineProvider {
    List<LineProfile> lineProfiles = new ArrayList<>();
    String file;
    int lineNo;
    String functionName;

    float totalTime;
    float maxLineTime;

    FunctionProfile(ProfileSchema.Function fnSchema, @Nullable String rootDirectory) {
        file = fnSchema.file;
        if (!Files.exists(Paths.get(file)) && rootDirectory != null && Files.exists(Paths.get(rootDirectory, file))) {
            // If file does not exist, maybe it does exist in the optional rootDirectory
            // In that case we use the file in the rootdirectory
            // This may happen for example when the run config has a working directory and
            // a script is NOT defined with an absolute path
            file = Paths.get(rootDirectory, file).toString();
        }
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

    public int getLineNrFromZero() {
        return lineNo - 1;
    }
}
