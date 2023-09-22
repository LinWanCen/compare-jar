package io.github.linwancen.compare;

import io.github.linwancen.util.CmdUtils;
import io.github.linwancen.util.CompressUtils;
import io.github.linwancen.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 2. 解压 jar 文件
 */
public class UnJar {
    private static final Logger LOG = LoggerFactory.getLogger(UnJar.class);

    /**
     * 2. 解压 jar 文件
     */
    public static Map<String, String> unJar(List<File> fileList) {
        Map<String, String> subPathMap = new ConcurrentHashMap<>();
        long t1 = System.currentTimeMillis();
        fileList.parallelStream().forEach(file -> {
            int baseLength = CompressUtils.base(file).length();
            if (file.exists()) {
                try (Stream<Path> walk = Files.walk(file.toPath())) {
                    walk.forEach(p-> unJarAdd(subPathMap, baseLength, p.toFile()));
                } catch (IOException e) {
                    LOG.info("fail to walk:{}", file. getAbsoluteFile(), e);
                }
            }
        });
        long t2 = System.currentTimeMillis();
        String fileName = fileList.stream().map(File::getName).limit(1).collect(Collectors.joining(", "));
        LOG.info("UnJar use {}, size: {}, fileName: {}", TimeUtils.useTime(t2 - t1), subPathMap.size(), fileName);
        return subPathMap;
    }

    /**
     * 解压 jar 文件并添加子目录
     */
    private static void unJarAdd(Map<String, String> subPathMap, int baseLength, File file) {
        if (file.isDirectory()) {
            return;
        }
        String path = file.getAbsolutePath();
        if (path.endsWith(".jar")) {
            String outPath = CompressUtils.un(file, f -> {
                String p = f.getAbsolutePath();
                String subPath = p.substring(baseLength);
                subPathMap.put(subPath, "");
            });
            if (outPath != null) {
                if (!CmdUtils.IS_WINDOWS) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        }
    }
}
