package io.github.linwancen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class JStackUtils {

    public static final String PID_AND_NAME = ManagementFactory.getRuntimeMXBean().getName();

    @SuppressWarnings("SpellCheckingInspection")
    public static final File SCRIPT_FILE = new File("jstack.sh");

    public static final String SCRIPT_FILE_CANONICAL_PATH = PathUtils.canonicalPath(SCRIPT_FILE);

    @SuppressWarnings("SpellCheckingInspection")
    public static final String JSTACK = "jstack";

    @SuppressWarnings("SpellCheckingInspection")
    public static final String CMD = JSTACK + " -l " + PID_AND_NAME + " >> jstack.log";

    private static final Logger LOG = LoggerFactory.getLogger(JStackUtils.class);

    private JStackUtils() {}

    public static void genJStackScripts() {
        try {
            Files.write(SCRIPT_FILE.toPath(), CMD.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.warn("genJStackScripts IOException\tfile:///{}", SCRIPT_FILE_CANONICAL_PATH, e);
        }
        if (!CmdUtils.IS_WINDOWS && SCRIPT_FILE.setExecutable(true, true)) {
            LOG.warn("setExecutable fail\tfile:///{}", SCRIPT_FILE_CANONICAL_PATH);
        }
    }

}
