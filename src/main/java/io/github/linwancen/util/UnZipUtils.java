package io.github.linwancen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UnZipUtils {

    // 防止 zip 炸弹 (后面改成可配置)
    /** 总文件阈值 */
    private static final int THRESHOLD_ENTRIES = 10_000;
    /** 总大小阈值 */
    private static final int THRESHOLD_SIZE = 1_000_000_000;
    /** 压缩比阈值 */
    private static final double THRESHOLD_RATIO = 10;

    private static class ZipBoomStat {
        // 总文件数
        int totalEntryArchive = 0;
        // 总文件大小
        int totalSizeArchive = 0;
    }

    private static final Logger LOG = LoggerFactory.getLogger(UnZipUtils.class);

    private UnZipUtils() {}

    public static List<File> unZip(File inFile, File outDir, String name) {
        return unZip(inFile, outDir, name, null, null);
    }

    /**
     * 排除优先于包含
     * <br/>
     */
    public static List<File> unZip(File inFile, File outDir, Pattern inclusion, Pattern exclusion) {
        return unZip(inFile, outDir, null, inclusion, exclusion);
    }

    private static List<File> unZip(File inFile, File outDir, String name,
                                    Pattern inclusion, Pattern exclusion) {
        ArrayList<File> list = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(inFile)) {
            // 在 unZipEntry(...) 方法会判断文件夹是否存在并创建所以这里就不创建了
            String outPath = outDir.getCanonicalPath();

            ZipBoomStat stat = new ZipBoomStat();

            if (name != null) {
                name = name.replace('\\', '/');
                while (name.startsWith("/")) {
                    name = name.substring(1);
                }
                ZipEntry entry = zipFile.getEntry(name);
                if (entry == null) {
                    LOG.debug("no found {}", name);
                    return list;
                }
                if (unZipEntry(list, zipFile, entry, outPath, stat)) {
                    LOG.debug("unzip {} fail.", name);
                }
                return list;
            }

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                name = entry.getName();
                // 有正则就筛选，没有就全部
                boolean isNotExclusion = exclusion == null || !exclusion.matcher(name).find();
                boolean isInclusion = inclusion == null || inclusion.matcher(name).find();
                if (isNotExclusion && isInclusion && unZipEntry(list, zipFile, entry, outPath, stat)) {
                    break;
                }
            }
        } catch (IOException e) {
            String path = PathUtils.canonicalPath(inFile);
            LOG.error("ZipFile IOException\tfile:///{}", path, e);
            // 这里就不返回 null 了，避免使用的人没判空导致空指针
            return list;
        }
        return list;
    }

    /**
     * @return 是否有异常
     */
    private static boolean unZipEntry(List<File> list, ZipFile zipFile, ZipEntry entry, String outPath,
                                      ZipBoomStat stat) {
        // 这个
        File outFile = new File(outPath, entry.getName());
        try {
            String unZipPath = outFile.getCanonicalPath();
            if (!unZipPath.startsWith(outPath)) {
                // 解决：路径注入漏洞(path injection vulnerabilities)-压缩滑动漏洞(zip slip vulnerabilities)
                // 防止任意文件访问 ../../../../../etc/password
                LOG.error("Entry is outside of the target directory\n  unZipPath: {}\n  outPath:\tfile:///{}",
                        unZipPath, outPath);
                return true;
            }
        } catch (IOException e) {
            LOG.error("outFile.getCanonicalPath IOException\n  outFile.getPath(): {}\n  outPath:\tfile:///{}",
                    outFile.getPath(), outPath);
            return true;
        }
        if (entry.isDirectory()) {
            if (!outFile.exists() && outFile.mkdirs()) {
                String path = PathUtils.canonicalPath(outFile);
                LOG.warn("unZipEntry mkdir fail\tfile:///{}", path);
            }
            list.add(outFile);
            return false;
        }
        PathUtils.mkdir(outFile.getParentFile());
        try (InputStream inputStream = zipFile.getInputStream(entry);
             OutputStream outputStream = new FileOutputStream(outFile)
        ) {
            byte[] buf1 = new byte[1024];
            int len;

            stat.totalEntryArchive++;
            // 当前文件大小，用 int 会报 integer division in floating-point context
            double totalSizeEntry = 0;
            while ((len = inputStream.read(buf1)) > 0) {
                outputStream.write(buf1, 0, len);

                totalSizeEntry += len;
                stat.totalSizeArchive += len;

                double compressionRatio = totalSizeEntry / entry.getCompressedSize();
                if (compressionRatio > THRESHOLD_RATIO) {
                    // 压缩和未压缩数据之间的比率非常可疑，看起来像是 Zip Bomb Attack
                    LOG.error("ratio between compressed and uncompressed data is highly suspicious, " +
                            "looks like a Zip Bomb Attack");
                    return true;
                }
            }
            if (stat.totalSizeArchive > THRESHOLD_SIZE) {
                // 未压缩的数据大小对于应用程序资源容量而言太大
                LOG.error("the uncompressed data size is too much for the application resource capacity");
                return true;
            }

            if (stat.totalEntryArchive > THRESHOLD_ENTRIES) {
                // 此档案中的条目过多，可能导致系统的inode耗尽
                LOG.error("too much entries in this archive, can lead to inodes exhaustion of the system");
                return true;
            }
            list.add(outFile);
            return false;
        } catch (IOException e) {
            String path = zipFile.getName().replace('\\', '/');
            LOG.error("unZipEntry IOException\tfile:///{}\n  {}", path, entry, e);
            return true;
        }
    }
}
