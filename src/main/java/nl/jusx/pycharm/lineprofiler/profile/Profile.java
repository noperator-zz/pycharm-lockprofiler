package nl.jusx.pycharm.lineprofiler.profile;


import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Profile {
    private static final Logger logger = Logger.getInstance(Profile.class.getName());

    List<FunctionProfile> functionProfiles = new ArrayList<>();
    private final float unit;
    private float totalTime;

    private Profile(ProfileSchema schema, @Nullable String rootDirectory) {
        unit = schema.unit;

        for (ProfileSchema.Function fSchema : schema.profiledFunctions) {
            FunctionProfile fn = new FunctionProfile(fSchema, rootDirectory);
            functionProfiles.add(fn);
            totalTime += fn.totalTime;
        }
    }

    public List<FunctionProfile> getProfiledFunctions() {
        return functionProfiles;
    }

    public String getUnitLong() {
        if (Float.valueOf(unit).equals(0.000001f)) {
            return "µs (microseconds)";
        } else if (Float.valueOf(unit).equals(0.001f)) {
            return "ms (milliseconds)";
        } else if (Float.valueOf(unit).equals(1f)) {
            return "s (seconds)";
        } else {
            return String.format("%.6f s", unit);
        }
    }

    public String getUnitShort() {
        if (Float.valueOf(unit).equals(0.000001f)) {
            return "µs";
        } else if (Float.valueOf(unit).equals(0.001f)) {
            return "ms";
        } else if (Float.valueOf(unit).equals(1f)) {
            return "s";
        } else {
            return "";
        }
    }

    public float getTotalTime() {
        return totalTime;
    }

    /**
     * Loads a .pclprof file into a Profile object
     *
     * The .pclprof file must have been created with our own python helper package line-profiler-pycharm.
     * The .pclprof file contains Json data and is parsed with the gson library
     *
     * @param profileFile .pclprof file to load
     * @return Profile object from .pclprof file
     */
    @Nullable
    public static Profile fromPclprof(String profileFile, @Nullable String rootDirectory) {
        Gson gson = new Gson();
        JsonReader reader;

        try {
            reader = new JsonReader(new FileReader(profileFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        ProfileSchema data = gson.fromJson(reader, ProfileSchema.class); // contains the whole reviews list

        return new Profile(data, rootDirectory);
    }

    @Nullable
    public static Profile fromPclprof(Path profileFile) {
        return fromPclprof(profileFile.toString(), profileFile.getParent().toString());
    }

    @Nullable
    public static Profile fromPclprof(VirtualFile profileFile) {
        return fromPclprof(profileFile.getPath(), profileFile.getParent().getPath());
    }
}
