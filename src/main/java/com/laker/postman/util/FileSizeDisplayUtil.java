package com.laker.postman.util;

/**
 * 文件大小显示工具类
 * 提供将字节数转换为更友好的显示格式
 */
public class FileSizeDisplayUtil {

    private FileSizeDisplayUtil() {
        // 私有构造函数，防止实例化
    }

    /**
     * 格式化文件大小显示，自动选择单位，保留两位小数，去除无意义的.00
     *
     * @param sizeBytes 字节数
     * @return 友好的显示字符串，如 1.23 MB
     */
    public static String formatSize(int sizeBytes) {
        String unit;
        double value;
        if (sizeBytes >= 1024 * 1024 * 1024) {
            unit = "GB";
            value = sizeBytes / (1024.0 * 1024 * 1024);
        } else if (sizeBytes >= 1024 * 1024) {
            unit = "MB";
            value = sizeBytes / (1024.0 * 1024);
        } else if (sizeBytes >= 1024) {
            unit = "KB";
            value = sizeBytes / 1024.0;
        } else {
            unit = "B";
            value = sizeBytes;
        }
        if (unit.equals("B")) {
            return String.format("%d %s", (int) value, unit);
        } else {
            return String.format("%.2f %s", value, unit);
        }
    }

    /**
     * 格式化文件下载进度显示
     *
     * @param totalBytes    已下载字节数
     * @param contentLength 总字节数
     * @return 友好的显示字符串
     */
    public static String formatDownloadSize(int totalBytes, int contentLength) {
        if (contentLength > 0) {
            return String.format("Downloaded: %s / %s", formatSize(totalBytes), formatSize(contentLength));
        } else {
            return String.format("Downloaded: %s", formatSize(totalBytes));
        }
    }
}
