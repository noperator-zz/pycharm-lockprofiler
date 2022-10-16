package nl.jusx.pycharm.lockprofiler.profile;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;

public class Profile {
    private static final Logger logger = Logger.getInstance(Profile.class.getName());
    private final ProfileSchema schema;

    private Profile(ProfileSchema schema, @Nullable String rootDirectory) {
        this.schema = schema;
    }

    public Map<String, Map<Integer, Map<Integer, ProfileSchema.LockStats>>> getProfiledFiles() {
        return schema.file_stats;
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
        ProfileSchema data = ProfileSchema.FromFile(profileFile); // contains the whole reviews list
        if (data == null) {
            return null;
        }

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
