package com.alibaba.datax.plugin.unstructuredstorage.writer.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * 这个是FTP处理文件夹的字节处理而从FTPwriter复制过来的
 */
public interface IFtpHelper {

    //使用被动方式
    void loginFtpServer(String host, String username, String password, int port, int timeout);

    void logoutFtpServer();

    /**
     * warn: 不支持递归创建, 比如 mkdir -p
     * */
    void mkdir(String directoryPath);

    /**
     * 支持目录递归创建
     */
    void mkDirRecursive(String directoryPath);

    OutputStream getOutputStream(String filePath, String encoding);

    OutputStream getOutputStreamForXlsx(String fileFullPath, String fileName, String suffix);

    void upLoad(String filePath, String fileName, String suffix);

    String getRemoteFileContent(String filePath);

    Set<String> getAllFilesInDir(String dir, String prefixFileName);

    /**
     * warn: 不支持文件夹删除, 比如 rm -rf
     * */
    void deleteFiles(Set<String> filesToDelete);
    
    void completePendingCommand();

    void writeByte(byte[] buffer, OutputStream outputStream, int divisor) throws IOException;

    boolean isDirExist(String directoryPath);

    boolean isFTPFileExist(String filePath);

    String buildFilePath(String path, String fileName);
}
