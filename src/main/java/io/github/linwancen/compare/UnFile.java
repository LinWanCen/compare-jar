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
 * 1. 解压非 jar 文件
 */
public class UnFile {
    private static final Logger LOG = LoggerFactory.getLogger(UnFile.class);

    /**
     * 1. 解压非 jar 文件
     */
    public static Map<String, String> unFile(List<File> fileList) {
        Map<String, String> subPathMap = new ConcurrentHashMap<>();
        long t1 = System.currentTimeMillis();
        fileList.parallelStream().forEach(file -> {
            int baseLength = CompressUtils.base(file).length();
            if (file.exists()) {
                try (Stream<Path> walk = Files.walk(file.toPath())) {
                    walk.forEach(p -> unFileAdd(subPathMap, baseLength, p.toFile(), true));
                } catch (IOException e) {
                    LOG.info("fail to walk:{}", file.getAbsoluteFile(), e);
                }
            }
        });
        long t2 = System.currentTimeMillis();
        String fileName = fileList.stream().map(File::getName).limit(1).collect(Collectors.joining(", "));
        LOG.info("UnFile use {}, size: {}, fileName: {}", TimeUtils.useTime(t2 - t1), subPathMap.size(), fileName);
        return subPathMap;
    }

    /**
     * 递归解压并添加子目录
     */
    private static void unFileAdd(Map<String, String> subPathMap, int baseLength, File file, boolean first) {
        if (file.isDirectory()) {
            return;
        }
        String path = file.getAbsolutePath();
        String outPath = null;
        if (path.endsWith(".jar")) {
            if (!first) {
                file = SuffixUtils.deleteSuffix(file);
                path = file.getAbsolutePath();
            }
            // 相同的 jar 包在这里解压会浪费太多时间，所以先比对二进制不同后再解压
        } else {
            // 递归
            outPath = CompressUtils.un(file, f -> unFileAdd(subPathMap, baseLength, f, false));
        }
        if (outPath != null) {
            if (!(first && CmdUtils.IS_WINDOWS)) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
            return;
        }
        String subPath = path.substring(baseLength);
        subPathMap.put(subPath, "");
    }
}
