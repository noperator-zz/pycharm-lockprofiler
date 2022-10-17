package nl.jusx.pycharm.lockprofiler.profile;

import com.google.gson.*;

import java.lang.reflect.Type;

import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileSchema {
    static class LockStatsAdapter implements JsonDeserializer<LockStats> {
        public LockStats deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) {
            long[] items = {0, 0, 0, 0, 0, 0, 0, 0};
            int i = 0;
            for (JsonElement ele : json.getAsJsonArray()) {
                items[i] = ele.getAsLong();
                i++;
            }
            return new LockStats(items[0], items[1], items[2], items[3], items[4], items[5], items[6], items[7]);
        }
    }
    public static class LockStats {
        public long hits;
        public long acquires;
        public long total_acquire_time;
        public long avg_acquire_time;
        public long max_acquire_time;
        public long total_hold_time;
        public long avg_hold_time;
        public long max_hold_time;

        public LockStats(
                long hits,
                long acquires,
                long total_acquire_time,
                long avg_acquire_time,
                long max_acquire_time,
                long total_hold_time,
                long avg_hold_time,
                long max_hold_time
        ) {
            this.hits = hits;
            this.acquires = acquires;
            this.total_acquire_time = total_acquire_time;
            this.avg_acquire_time = avg_acquire_time;
            this.max_acquire_time = max_acquire_time;
            this.total_hold_time = total_hold_time;
            this.avg_hold_time = avg_hold_time;
            this.max_hold_time = max_hold_time;
        }

//        public Long[] asArray() {
//            return new Long[] {
//                hits,
//                acquires,
//                total_acquire_time,
//                avg_acquire_time,
//                max_acquire_time,
//                total_hold_time,
//                avg_hold_time,
//                max_hold_time
//            };
//        }
    }

    public Map<Long, LockStats> lock_stats;
    private Map<Long, String> lock_hashes;
    public Map<String, Map<Integer, Map<Long, LockStats>>> file_stats;

    public String getLockName(long lock_hash) {
        return lock_hashes.get(lock_hash);
    }

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
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LockStats.class, new LockStatsAdapter())
                .create();
        JsonReader reader;

        try {
            reader = new JsonReader(new FileReader(profileFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        ProfileSchema profile = gson.fromJson(reader, ProfileSchema.class);
        return profile;
    }

}



