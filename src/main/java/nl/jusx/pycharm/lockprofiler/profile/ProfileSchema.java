package nl.jusx.pycharm.lockprofiler.profile;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

public class ProfileSchema {
    public static class LockStats {
        int hits;
        int total_acquire_time;
        int avg_acquire_time;
        int max_acquire_time;
        int total_hold_time;
        int avg_hold_time;
        int max_hols_time;
    }

    private Map<Integer, LockStats> lock_stats;
    private Map<Integer, String> lock_hashes;
    public Map<String, Map<Integer, Map<Integer, LockStats>>> file_stats;


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

        return gson.fromJson(reader, ProfileSchema.class);
    }
}



