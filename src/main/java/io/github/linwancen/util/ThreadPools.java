package io.github.linwancen.util;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ThreadPools {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadPools.class);
    private static final LinkedHashMap<String, ScheduledExecutorService> poolMap = new LinkedHashMap<>();

    private ThreadPools() {}

    /**
     * 获取指定名字和核心倍数的定时线程池
     * <br/>ThreadsPools.get("name-%d", 4).execute(new Runnable() {})
     */
    public static ScheduledExecutorService get(String namingPattern, double threadMultiplier) {
        ScheduledExecutorService executor = poolMap.get(namingPattern);
        if (executor != null) {
            return executor;
        }
        BasicThreadFactory threadFactory = new BasicThreadFactory.Builder()
                .namingPattern(namingPattern)
                .daemon(true)
                .build();
        // availableProcessors() 该值在特定的虚拟机调用期间可能发生更改。
        // 因此，对可用处理器数目很敏感的应用程序应该不定期地轮询该属性，并相应地调整其资源用法。
        int corePoolSize = (int) (Runtime.getRuntime().availableProcessors() * threadMultiplier);
        executor = new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
        poolMap.put(namingPattern, executor);
        // 虽然 JStackUtils.CMD 在当前进程固定，但是在没用到线程池时就不打印，打印是为了方便排查卡住原因
        LOG.info("create threads pool\tsize:{}, namingPattern: {}, {} script:\t{}",
                corePoolSize, namingPattern, JStackUtils.JSTACK, JStackUtils.CMD);
        // 生成脚本方便不看日志执行
        JStackUtils.genJStackScripts();
        return executor;
    }
}
