package io.github.linwancen.compare;

import io.github.linwancen.util.CompressUtils;
import io.github.linwancen.util.TimeUtils;
import io.github.linwancen.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 二进制比对
 */
public class Diff {
    private static final Logger LOG = LoggerFactory.getLogger(Diff.class);

    /**
     * 二进制比对
     */
    public static void diffByte(List<File> fileList, Map<String, String> subPathMap,
                                               TriConsumer<Boolean, String, List<File>> fun) {
        long t1 = System.currentTimeMillis();
        subPathMap.entrySet().parallelStream().forEach(entry -> {
            List<File> subFiles = new ArrayList<>();
            byte[] bytes1 = null;
            boolean diff = false;
            for (File f : fileList) {
                String basePath = CompressUtils.base(f);
                String path = basePath + entry.getKey();
                File file = new File(path);
                subFiles.add(file);
                if (diff || !file.exists()) {
                    diff = true;
                    continue;
                }
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    if (bytes1 == null) {
                        bytes1= bytes;
                    } else {
                        if (!Arrays.equals(bytes1, bytes)) {
                            diff = true;
                        }
                    }
                } catch (IOException e) {
                    LOG.error("fail read: {}", path, e);
                }
            }
            fun.accept(diff, entry.getKey(), subFiles);
        });
        long t2 = System.currentTimeMillis();
        String fileName = fileList.stream().map(File::getName).limit(1).collect(Collectors.joining(", "));
        String useTime = TimeUtils.useTime(t2 - t1);
        LOG.info("Diff use {}, size: {}, fileName: {}", useTime, subPathMap.size(), fileName);
    }
}
