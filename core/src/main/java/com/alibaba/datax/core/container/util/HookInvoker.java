package com.alibaba.datax.core.container.util;

/**
 * Created by xiafei.qiuxf on 14/12/17.
 */

import com.alibaba.datax.common.exception.CommonErrorCode;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.spi.Hook;
import com.alibaba.datax.common.statistics.JobStatistics;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.util.FrameworkErrorCode;
import com.alibaba.datax.core.util.container.ClassLoaderSwapper;
import com.alibaba.datax.core.util.container.JarLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * 扫描给定目录的所有一级子目录，每个子目录当作一个Hook的目录。
 * 对于每个子目录，必须符合ServiceLoader的标准目录格式，见http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html。
 * 加载里头的jar，使用ServiceLoader机制调用。
 */
public class HookInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(HookInvoker.class);
    private final Map<String, Number> msg;
    private final Configuration conf;
    private final JobStatistics jobStatistics;

    private ClassLoaderSwapper classLoaderSwapper = ClassLoaderSwapper
            .newCurrentThreadClassLoaderSwapper();

    private File baseDir;

    public HookInvoker(String baseDirName, Configuration conf, Map<String, Number> msg, JobStatistics jobStatistics) {
        this.baseDir = new File(baseDirName);
        this.conf = conf;
        this.msg = msg;
        this.jobStatistics = jobStatistics;
    }

    public void invokeAll() throws IOException {
        try {
            if (!baseDir.exists() || baseDir.isFile()) {
                LOG.info("No hook invoked, because base dir not exists or is a file: " + baseDir.getAbsolutePath());
                return;
            }
            String[] subDirs = baseDir.list();
/*
        String[] subDirs = baseDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });
*/
            if (subDirs == null) {
                throw DataXException.asDataXException(FrameworkErrorCode.HOOK_LOAD_ERROR, "获取HOOK子目录返回null");
            }

            for (String subDir : subDirs) {
                //doInvoke(new File(baseDir, subDir).getAbsolutePath());
            }
        }catch (Exception e){
            LOG.info(e.getMessage());
        }
    }

    /**
     * 遍历path路径下的所有class文件，解析。这些class都是hook的实现类，然后执行invoke实现方法
     * 好处：可以无限制的添加hook的实现类
     * @param path
     */
    private void doInvoke(String path) throws IOException {

        //path="D:\\datax\\plugin\\hook\\datatech";
        LOG.info("HOOK-jar------------------------------------------------------------path:"+path);
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            //jar包的
            JarLoader jarLoader = null;
            jarLoader = new JarLoader(new String[]{path});

            // URLClassLoader 进行jar包的加载
            classLoaderSwapper.setCurrentThreadClassLoader(jarLoader);
            Iterator<Hook> hookIt = ServiceLoader.load(Hook.class).iterator();
            if (!hookIt.hasNext()) {
                LOG.warn("No hook defined under path: {}", path);
            } else {
                while (hookIt.hasNext()) {
                    Hook hook = hookIt.next();
                    //LOG.info("Invoke hook [{}], path: {}", hook.getName(), path);
                    hook.invoke(conf, msg, jobStatistics);
                }
            }
        } catch (Exception e) {
            LOG.error("Exception when invoke hook");
            throw DataXException.asDataXException(
                    CommonErrorCode.HOOK_INTERNAL_ERROR, "Exception when invoke hook", e);
        } finally {
            classLoaderSwapper.restoreCurrentThreadClassLoader();
        }
    }

    public static void main(String[] args) throws IOException {
        new HookInvoker("/Users/xiafei/workspace/datax3/target/datax/datax/hook",
                null, new HashMap<>(4), null).invokeAll();
    }
}
