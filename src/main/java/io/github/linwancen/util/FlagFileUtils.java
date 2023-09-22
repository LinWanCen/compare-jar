package io.github.linwancen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class FlagFileUtils {

    @SuppressWarnings("SpellCheckingInspection")
    public static final String SUCC_SUFFIX = ".succ";
    public static final String FAIL_SUFFIX = ".fail";
    private static final Logger LOG = LoggerFactory.getLogger(FlagFileUtils.class);

    private FlagFileUtils() {}

    public static void update(String pathPrefix, String suffix, String... deleteSuffixes) {
        for (String s : deleteSuffixes) {
            PathUtils.deleteFile(new File(pathPrefix + s));
        }
        File file = new File(pathPrefix + suffix);
        PathUtils.deleteFile(file);
        try {
            if (file.createNewFile()) {
                return;
            }
            String dirSpaceName = PathUtils.dirSpaceName(PathUtils.canonicalPath(file));
            LOG.warn("have not create\tfile:///{}", dirSpaceName);
        } catch (IOException e) {
            String dirSpaceName = PathUtils.dirSpaceName(PathUtils.canonicalPath(file));
            LOG.warn("Exception for create\tfile:///{}", dirSpaceName, e);
        }
    }
}
