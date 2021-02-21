package com.jusx.pycharm.colored_lineprofiler.profile;


import java.util.ArrayList;
import java.util.List;

public class Profile {
    List<FunctionProfile> functionProfiles = new ArrayList<>();
    private final float unit;
    private float totalTime;

    public Profile(ProfileSchema schema) {
        unit = schema.unit;

        for (ProfileSchema.Function fSchema : schema.profiledFunctions) {
            FunctionProfile fn = new FunctionProfile(fSchema);
            functionProfiles.add(fn);
            totalTime += fn.totalTime;
        }
    }

    public List<FunctionProfile> getProfiledFunctions() {
        return functionProfiles;
    }

    public float getUnit() {
        return unit;
    }

    public float getTotalTime() {
        return totalTime;
    }
}
