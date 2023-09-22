package io.github.linwancen.util;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.function.Consumer;

/**
 * Apache 解压
 */
public class CompressUtils {

    /**
     * 解压
     */
    public static String un(File file, Consumer<File> fun) {
        String path = file.getAbsolutePath();
        int i = path.lastIndexOf('.');
        String base = i > 0 ? path.substring(0, i) : path;
        String ext = i > 0 ? path.substring(i + 1) : "";
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(ext, bis)
        ) {
            ArchiveEntry entry;
            while ((entry = ais.getNextEntry()) != null) {
                if (entry.isDirectory() || !ais.canReadEntryData(entry)) {
                    continue;
                }
                String name = entry.getName();
                File outFile = new File(base, name);
                File parentFile = outFile.getParentFile();
                if (parentFile != null) {
                    //noinspection ResultOfMethodCallIgnored
                    parentFile.mkdirs();
                }
                try (OutputStream o = Files.newOutputStream(outFile.toPath())) {
                    IOUtils.copy(ais, o);
                }
                fun.accept(outFile);
            }
            return base;
        } catch (ArchiveException ignore) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 去除压缩文件后缀
     */
    public static String base(File file) {
        String path = file.getAbsolutePath();
        if (path.endsWith(".tar") || path.endsWith(".zip") || path.endsWith(".jar")) {
            path = path.substring(0, path.length() - 4);
        }
        return path;
    }
}
