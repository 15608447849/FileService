package server.comm;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

/**
 * 系统环境工具类
 * Ref. https://docs.oracle.com/javase/10/docs/api/com/sun/management/OperatingSystemMXBean.html#method.summary
 */
public class RuntimeUtil {

    private static final OperatingSystemMXBean systemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final Runtime runtime = Runtime.getRuntime();

    /**
     * 获取物理内存总大小
     *
     * @return
     */
    public static long getTotalPhysicalMemorySize() {
        return systemMXBean.getTotalPhysicalMemorySize();
    }

    /**
     * 获取物理内存剩余大小
     *
     * @return
     */
    public static long getFreePhysicalMemorySize() {
        return systemMXBean.getFreePhysicalMemorySize();
    }

    /**
     * 获取物理内存已使用大小
     *
     * @return
     */
    public static long getUsedPhysicalMemorySize() {
        return systemMXBean.getTotalPhysicalMemorySize() - systemMXBean.getFreePhysicalMemorySize();
    }

    /**
     * 获取 Swap 总大小
     *
     * @return
     */
    public static long getTotalSwapSpaceSize() {
        return systemMXBean.getTotalSwapSpaceSize();
    }

    /**
     * 获取 Swap 剩余大小
     *
     * @return
     */
    public static long getFreeSwapSpaceSize() {
        return systemMXBean.getFreeSwapSpaceSize();
    }

    /**
     * 获取 Swap 已使用大小
     *
     * @return
     */
    public static long getUsedSwapSpaceSize() {
        return systemMXBean.getTotalSwapSpaceSize() - systemMXBean.getFreeSwapSpaceSize();
    }

    /**
     * 获取 JVM 最大内存
     *
     * @return
     */
    public static long getJvmMaxMemory() {
        return runtime.maxMemory();
    }

    /**
     * 获取 JVM 内存总大小
     *
     * @return
     */
    public static long getJvmTotalMemory() {
        return runtime.totalMemory();
    }

    /**
     * 获取 JVM 内存剩余大小
     *
     * @return
     */
    public static long getJvmFreeMemory() {
        return runtime.freeMemory();
    }

    /**
     * 获取 JVM 内存已使用大小
     *
     * @return
     */
    public static long getJvmUsedMemory() {
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * 获取系统 CPU 使用率
     *
     * @return
     */
    public static double getSystemCpuLoad() {
        return systemMXBean.getSystemCpuLoad();
    }

    /**
     * 获取 JVM 进程 CPU 使用率
     *
     * @return
     */
    public static double getProcessCpuLoad() {
        return systemMXBean.getProcessCpuLoad();
    }

    public static String byteLength2StringShow(long size) {
        //如果字节数少于1024，则直接以B为单位，否则先除于1024，后3位因太少无意义
        if (size < 1024) {
            return size + "B";
        } else {
            size = size / 1024;
        }
        //如果原字节数除于1024之后，少于1024，则可以直接以KB作为单位
        //因为还没有到达要使用另一个单位的时候
        //接下去以此类推
        if (size < 1024) {
            return size + "KB";
        } else {
            size = size / 1024;
        }
        if (size < 1024) {
            //因为如果以MB为单位的话，要保留最后1位小数，
            //因此，把此数乘以100之后再取余
            size = size * 100;
            return (size / 100) + "."
                    + (size % 100) + "MB";
        } else {
            //否则如果要以GB为单位的，先除于1024再作同样的处理
            size = size * 100 / 1024;
            return (size / 100) + "."
                    + (size % 100) + "GB";
        }
    }


}
