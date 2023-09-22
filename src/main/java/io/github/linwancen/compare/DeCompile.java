package io.github.linwancen.compare;

import io.github.linwancen.util.CmdUtils;
import io.github.linwancen.util.CompressUtils;
import io.github.linwancen.util.PathUtils;
import io.github.linwancen.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 3. 反编译
 */
public class DeCompile {
    private static final Logger LOG = LoggerFactory.getLogger(DeCompile.class);

    /**
     * 3. 反编译
     */
    public static Map<String, String> deCompile(List<File> fileList, Map<String, String> subPath3) {
        Map<String, String> subPathMap = new ConcurrentHashMap<>();
        long t1 = System.currentTimeMillis();
        subPath3.entrySet().parallelStream().forEach(entry -> {
            for (File f : fileList) {
                String basePath = CompressUtils.base(f);
                int baseLength = basePath.length();
                File file = new File(basePath, entry.getKey());
                if (!file.exists()) {
                    continue;
                }
                String path = file.getAbsolutePath();
                if (path.endsWith(".class")) {
                    String deCompilePath = path + ".java";
                    long t3 = System.currentTimeMillis();
                    CmdUtils.exec("javap -c \"" + path + "\"", 30_000, (out, err) -> {
                        String code = out.toString();
                        int i = code.indexOf('\n');
                        if (i > 0) {
                            code = code.substring(i);
                        }
                        File deCompileFile = new File(deCompilePath);
                        File parentFile = deCompileFile.getParentFile();
                        PathUtils.mkdir(parentFile);
                        try {
                            Files.write(deCompileFile.toPath(), code.getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            LOG.error("fail to write({})", deCompilePath, e);
                        }
                    });
                    long t4 = System.currentTimeMillis();
                    LOG.trace("CmdUtils use {}, {}/{}", TimeUtils.useTime(t4 - t3), subPathMap.size(), subPath3.size());
                    String subPath = deCompilePath.substring(baseLength);
                    subPathMap.put(subPath, "");
                } else {
                    String subPath = path.substring(baseLength);
                    subPathMap.put(subPath, "");
                }
            }
        });
        long t2 = System.currentTimeMillis();
        String fileName = fileList.stream().map(File::getName).limit(1).collect(Collectors.joining(", "));
        String useTime = TimeUtils.useTime(t2 - t1);
        LOG.info("DeCompile use {}, size: {}, fileName: {}", useTime, subPathMap.size(), fileName);
        return subPathMap;
    }
}
