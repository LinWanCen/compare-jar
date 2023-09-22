package io.github.linwancen.compare;

import java.io.File;
import java.util.regex.Pattern;

/**
 * 删除 jar 文件时间戳
 */
public class SuffixUtils {
    public static final Pattern DEL_PATTERN = Pattern.compile("-\\d.*");

    /**
     * 删除 jar 文件时间戳
     */
    public static File deleteSuffix(File file) {
        File parent = file.getParentFile();
        String oldName = file.getName();
        int extIndex = oldName.lastIndexOf('.');
        if (oldName.endsWith(".pom.sha1")) {
            extIndex = oldName.length() - 9;
        }
        String oldNameNotExt = extIndex >= 0 ? oldName.substring(0, extIndex) : oldName;
        String ext = extIndex >= 0 ? oldName.substring(extIndex) : "";
        String newNameNotExt = DEL_PATTERN.matcher(oldNameNotExt).replaceAll("");
        File newFile = new File(parent, newNameNotExt + ext);
        if (file.renameTo(newFile)) {
            return newFile;
        }
        return file;
    }
}
