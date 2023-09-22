package io.github.linwancen.util;

import java.io.File;

/**
 * 递归删除空目录
 */
public class EmptyDirUtils {

    /**
     * 递归删除空目录
     */
    public static void delete(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                delete(f);
            }
        }
        File[] newFiles = dir.listFiles();
        if (newFiles == null) {
            return;
        }
        if (newFiles.length == 0) {
            //noinspection ResultOfMethodCallIgnored
            dir.delete();
        }
    }
}
