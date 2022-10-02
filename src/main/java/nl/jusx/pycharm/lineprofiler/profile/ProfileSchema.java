package nl.jusx.pycharm.lineprofiler.profile;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class ProfileSchema {
    static class Function {
        static class Line {
            int lineNo;
            long hits;
            long time;

        }

        String file;
        int lineNo;
        String functionName;
        Function.Line[] profiledLines;
    }

    Function[] profiledFunctions;
    private float unit;


    private static final int numUnits = 4;
    private static final String[] unitLookup = {"%.0f ns", "%.3f \u03BCs", "%.3f ms", "%.3f s"};

    public static String formatTime(long time) {
        double timeNs = time;

        int idx = 0;
        while (idx < numUnits && timeNs > 1000) {
            idx++;
            timeNs /= 1000;
        }

        return String.format(
                unitLookup[idx],
                timeNs
        );
    }

    public static ProfileSchema FromFile(String profileFile) {
        Gson gson = new Gson();
        JsonReader reader;

        try {
            reader = new JsonReader(new FileReader(profileFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        ProfileSchema data = gson.fromJson(reader, ProfileSchema.class); // contains the whole reviews list

        data.fixUnits();
        return data;
    }

    private void fixUnits() {
        // convert all the time units to nanoseconds
        for (Function f : profiledFunctions) {
            for (Function.Line l : f.profiledLines) {
                l.time *= unit * 1000000000;
            }
        }
    }
}



