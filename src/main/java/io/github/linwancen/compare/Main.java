package io.github.linwancen.compare;

import io.github.linwancen.util.CompressUtils;
import io.github.linwancen.util.EmptyDirUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * 【主程序】组装逻辑
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        List<File> fileList = new ArrayList<>();
        for (String arg : args) {
            fileList.add(new File(arg));
        }
        diff(fileList);
    }

    public static void diff(List<File> fileList) {
        LOG.info("CommonPoolParallelism:{}", ForkJoinPool.getCommonPoolParallelism());
        // 差异文件清单，最后输出用
        List<String> diffList = Collections.synchronizedList(new ArrayList<>());
        // 差异的 jar 文件
        List<List<File>> jarList = new ArrayList<>();
        Map<String, String> subPath = UnFile.unFile(fileList);
        diffNotJar(fileList, subPath, jarList, diffList);

        for (List<File> fileList2 : jarList) {
            // 差异的 class 文件
            Map<String, String> classList = new ConcurrentHashMap<>();
            Map<String, String> subPath2 = UnJar.unJar(fileList2);
            diffJar(fileList2, subPath2, classList, diffList);

            Map<String, String> subPath3 = DeCompile.deCompile(fileList2, classList);
            diffClass(fileList2, subPath3, diffList);
        }

        fileList.parallelStream().forEach(dir -> {
            File baseDir = new File(CompressUtils.base(dir));
            EmptyDirUtils.delete(baseDir);
        });

        writeDiffPaths(fileList, diffList);
    }

    /**
     * 1. 比对非 jar 文件
     * <br>并记录不同的 jar 文件
     */
    private static void diffNotJar(List<File> fileList, Map<String, String> subPath, List<List<File>> jarList, List<String> diffList) {
        Diff.diffByte(fileList, subPath, (diff, p, files) -> {
            if (diff) {
                if (p.endsWith(".jar") && files.size() > 1) {
                    jarList.add(files);
                } else {
                    files.stream().map(File::getAbsolutePath).forEach(diffList::add);
                }
            } else {
                //noinspection ResultOfMethodCallIgnored
                files.forEach(File::delete);
            }
        });
    }

    /**
     * 2. 比对 jar 内的文件
     * <br>并记录不同的 class 文件
     */
    private static void diffJar(List<File> fileList2, Map<String, String> subPath2, Map<String, String> classList, List<String> diffList) {
        Diff.diffByte(fileList2, subPath2, (diff, p, files) -> {
            if (diff) {
                if (p.endsWith(".class") && files.size() > 1) {
                    classList.put(p, "");
                } else {
                    files.stream().map(File::getAbsolutePath).forEach(diffList::add);
                }
            } else {
                //noinspection ResultOfMethodCallIgnored
                files.forEach(File::delete);
            }
        });
    }

    /**
     * 3. 比对 class 的源码
     */
    private static void diffClass(List<File> fileList2, Map<String, String> subPath3, List<String> diffList) {
        Diff.diffByte(fileList2, subPath3, (diff, p, files) -> {
            // 删除源码文件
            //noinspection ResultOfMethodCallIgnored
            files.forEach(File::delete);
            // 获取 class 文件路径
            List<String> classFilePathList = files.stream()
                    .map(File::getAbsolutePath)
                    .map(s -> s.substring(0, s.length() - 5))
                    .collect(Collectors.toList());
            if (diff) {
                diffList.addAll(classFilePathList);
            } else {
                // 删除没有差异的 class 文件
                //noinspection ResultOfMethodCallIgnored
                classFilePathList.forEach(s -> new File(s).delete());
            }
        });
    }

    /**
     * 输出差异文件清单
     */
    private static void writeDiffPaths(List<File> fileList, List<String> diffList) {
        String diffStr = String.join("\n", diffList);
        String diffPath = "diffPaths.txt";
        if (!fileList.isEmpty()) {
            String base = CompressUtils.base(fileList.get(0));
            File file = new File(base).getParentFile();
            diffPath = file.getAbsolutePath() + '/' + diffPath;
        }
        File file = new File(diffPath);
        try {
            Files.write(file.toPath(), diffStr.getBytes(StandardCharsets.UTF_8));
            LOG.info("diff list file: \nfile:///{}\n", file.getAbsolutePath().replace('\\', '/'));
        } catch (Exception e) {
            LOG.error("write diff fail:\nfile:///{}\n", file.getAbsolutePath().replace('\\', '/'), e);
        }
    }
}
