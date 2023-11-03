package com.alibaba.datax.plugin.writer.ftpwriter.util;

import java.io.*;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.plugin.writer.ftpwriter.FtpWriterErrorCode;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class StandardFtpHelperImpl implements IFtpHelper {

    //本地字符编码
    static String LOCAL_CHARSET = "GBK";

    // FTP协议里面，规定文件名编码为iso-8859-1
    static String SERVER_CHARSET = "ISO-8859-1";

    private static final Logger LOG = LoggerFactory
            .getLogger(StandardFtpHelperImpl.class);
    FTPClient ftpClient = null;

    @Override
    public void loginFtpServer(String host, String username, String password,
            int port, int timeout) {
        this.ftpClient = new FTPClient();
        try {


            // 不需要写死ftp server的OS TYPE,FTPClient getSystemType()方法会自动识别
            // this.ftpClient.configure(new FTPClientConfig(FTPClientConfig.SYST_UNIX));
            this.ftpClient.setDefaultTimeout(timeout);
            this.ftpClient.setConnectTimeout(timeout);
            this.ftpClient.setDataTimeout(timeout);

            // 连接登录
            this.ftpClient.connect(host, port);
            this.ftpClient.login(username, password);

            //LOG.info("ftpClient.getSystemType()==============================="+ftpClient.getSystemType());

            this.ftpClient.enterRemotePassiveMode();
            this.ftpClient.enterLocalPassiveMode();
            int reply = this.ftpClient.getReplyCode();
            //LOG.info("reply==============================="+reply);
            if (!FTPReply.isPositiveCompletion(reply)) {
                this.ftpClient.disconnect();
                String message = String
                        .format("与ftp服务器建立连接失败,host:%s, port:%s, username:%s, replyCode:%s",
                                host, port, username, reply);
                LOG.error(message);
                throw DataXException.asDataXException(
                        FtpWriterErrorCode.FAIL_LOGIN, message);
            }

            //设置命令传输编码
            //file.encoding的值保存的是每个程序的main入口的那个java文件的保存编码，是.java文件的编码。
            String fileEncoding = System.getProperty("file.encoding");
            //LOG.info("fileEncoding==============================="+fileEncoding);
            this.ftpClient.setControlEncoding(fileEncoding);
        } catch (UnknownHostException e) {
            String message = String.format(
                    "请确认ftp服务器地址是否正确，无法连接到地址为: [%s] 的ftp服务器, errorMessage:%s",
                    host, e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.FAIL_LOGIN, message, e);
        } catch (IllegalArgumentException e) {
            String message = String.format(
                    "请确认连接ftp服务器端口是否正确，错误的端口: [%s], errorMessage:%s", port,
                    e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.FAIL_LOGIN, message, e);
        } catch (Exception e) {
            String message = String
                    .format("与ftp服务器建立连接失败,host:%s, port:%s, username:%s, errorMessage:%s",
                            host, port, username, e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.FAIL_LOGIN, message, e);
        }

    }

    @Override
    public void logoutFtpServer() {
        if (this.ftpClient.isConnected()) {
            try {
                this.ftpClient.logout();
            } catch (IOException e) {
                String message = String.format(
                        "与ftp服务器断开连接失败, errorMessage:%s", e.getMessage());
                LOG.error(message);
                throw DataXException.asDataXException(
                        FtpWriterErrorCode.FAIL_DISCONNECT, message, e);
            } finally {
                if (this.ftpClient.isConnected()) {
                    try {
                        this.ftpClient.disconnect();
                    } catch (IOException e) {
                        String message = String.format(
                                "与ftp服务器断开连接失败, errorMessage:%s",
                                e.getMessage());
                        LOG.error(message);
                        throw DataXException.asDataXException(
                                FtpWriterErrorCode.FAIL_DISCONNECT, message, e);
                    }
                }
                this.ftpClient = null;
            }
        }
    }

    @Override
    public void mkdir(String directoryPath) {
        String message = String.format("创建目录:%s时发生异常,请确认与ftp服务器的连接正常,拥有目录创建权限",
                directoryPath);
        try {
            this.printWorkingDirectory();
            boolean isDirExist = this.ftpClient
                    .changeWorkingDirectory(directoryPath);
            if (!isDirExist) {
                int replayCode = this.ftpClient.mkd(directoryPath);
                message = String
                        .format("%s,replayCode:%s", message, replayCode);
                if (replayCode != FTPReply.COMMAND_OK
                        && replayCode != FTPReply.PATHNAME_CREATED) {
                    throw DataXException.asDataXException(
                            FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION,
                            message);
                }
            }
        } catch (IOException e) {
            message = String.format("%s, errorMessage:%s", message,
                    e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
        }
    }

    /**
     * 递归创建地址
     * @param directoryPath
     */
    @Override
    public void mkDirRecursive(String directoryPath){
        LOG.info("这里需要创建或者配好的的目录--------------------------"+directoryPath);
        StringBuilder dirPath = new StringBuilder();
        dirPath.append(IOUtils.DIR_SEPARATOR_UNIX);
        String[] dirSplit = StringUtils.split(directoryPath,IOUtils.DIR_SEPARATOR_UNIX);
        String message = String.format("创建目录:%s时发生异常,请确认与ftp服务器的连接正常,拥有目录创建权限", directoryPath);
        try {
            // ftp server不支持递归创建目录,只能一级一级创建
            for(String dirName : dirSplit){
                dirPath.append(dirName);
                boolean mkdirSuccess = mkDirSingleHierarchy(dirPath.toString());
                dirPath.append(IOUtils.DIR_SEPARATOR_UNIX);
                if(!mkdirSuccess){
                    throw DataXException.asDataXException(
                            FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION,
                            message);
                }
            }
        } catch (IOException e) {
            message = String.format("%s, errorMessage:%s", message,
                    e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
        }
    }

    public boolean mkDirSingleHierarchy(String directoryPath) throws IOException {

        // 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码（GBK）.
        if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {
            LOCAL_CHARSET = "UTF-8";
            //LOG.info("系统格式是=====================================================UTF-8");
        }
        ftpClient.setControlEncoding(LOCAL_CHARSET);
        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
        ftpClient.enterLocalPassiveMode();
        String ftpPath2 = new String(directoryPath.getBytes(LOCAL_CHARSET),FTPClient.DEFAULT_CONTROL_ENCODING);

        boolean isDirExist = this.ftpClient
                .changeWorkingDirectory(ftpPath2);
        // 如果directoryPath目录不存在,则创建
        if (!isDirExist) {
            int replayCode = this.ftpClient.mkd(directoryPath);
            if (replayCode != FTPReply.COMMAND_OK
                    && replayCode != FTPReply.PATHNAME_CREATED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 20210714-wx
     * @param filePath
     * @return
     */
    @Override
    public OutputStream getOutputStream(String filePath) {
        try {
            this.printWorkingDirectory();
            String parentDir = filePath.substring(0,
                    StringUtils.lastIndexOf(filePath, IOUtils.DIR_SEPARATOR));
            this.ftpClient.changeWorkingDirectory(new String(parentDir.getBytes("GBK"),FTP.DEFAULT_CONTROL_ENCODING));
            this.printWorkingDirectory();

            String ftpPath2 = new String(filePath.getBytes("GBK"),FTP.DEFAULT_CONTROL_ENCODING);

            this.ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            OutputStream writeOutputStream = this.ftpClient.appendFileStream(ftpPath2);
            String message = String.format(
                    "打开FTP文件[%s]获取写出流时出错,请确认文件%s有权限创建，有权限写出等", filePath,
                    filePath);
            if (null == writeOutputStream) {
                throw DataXException.asDataXException(
                        FtpWriterErrorCode.OPEN_FILE_ERROR, message);
            }

            return writeOutputStream;
        } catch (IOException e) {
            String message = String.format(
                    "写出文件 : [%s] 时出错,请确认文件:[%s]存在且配置的用户有权限写, errorMessage:%s",
                    filePath, filePath, e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.OPEN_FILE_ERROR, message);
        }
    }

    /**
     * 在ftp模式下时候，如果选择了.xlsx模式导出
     * 20220408-wx
     * @param filePath
     * @return
     */
    @Override
    public OutputStream getOutputStreamForXlsx(String filePath,String fileName,String suffix) {
        try {
            this.printWorkingDirectory();
            String parentDir = filePath.substring(0,
                    StringUtils.lastIndexOf(filePath, IOUtils.DIR_SEPARATOR));
            this.ftpClient.changeWorkingDirectory(new String(parentDir.getBytes("GBK"),FTP.DEFAULT_CONTROL_ENCODING));
            this.printWorkingDirectory();

            //String ftpPath2 = new String(filePath.getBytes("GBK"),FTP.DEFAULT_CONTROL_ENCODING);

            File file = new File("datax"+File.separator+"tmp"+File.separator+"ftp"+File.separator+fileName+suffix);
            if (!file.exists()) {//如果没有这个file文件那就自己建立一个
                file.createNewFile();//有路径才能创建文件
            }

            LOG.info(file.getAbsolutePath());
            System.out.println(file.getAbsolutePath());

            FileOutputStream writeOutputStream = new FileOutputStream(file);

            String message = String.format(
                    "打开FTP文件[%s]获取写出流时出错,请确认文件%s有权限创建，有权限写出等", filePath,
                    filePath);
            if (null == writeOutputStream) {
                throw DataXException.asDataXException(
                        FtpWriterErrorCode.OPEN_FILE_ERROR, message);
            }

            return writeOutputStream;
        } catch (IOException e) {
            String message = String.format(
                    "写出文件 : [%s] 时出错,请确认文件:[%s]存在且配置的用户有权限写, errorMessage:%s",
                    filePath, filePath, e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.OPEN_FILE_ERROR, message);
        }
    }

    /**
     * 临时文件的存放datax/tmp/ftp
     * 在ftp模式下时候，如果选择了.xlsx模式导出
     * 20220408-wx
     * @param filePath ftp的路径
     * @param fileName 本地文件名称
     * @param suffix 文件后缀名.xlsx
     * @return
     */
    @Override
    public void upLoad(String filePath,String fileName,String suffix) {
        try {
            File file = new File("datax"+File.separator+"tmp"+File.separator+"ftp"+File.separator+fileName+suffix);
            FileInputStream fileInputStream = new FileInputStream(file);
            //写入ftp
            //文件格式一定要有！！！！！！！
            this.ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            this.ftpClient.storeFile(filePath,fileInputStream);
            //删掉临时文件
            //file.delete();
        } catch (IOException e) {
            String message = String.format(
                    "写出文件 : [%s] 时出错,请确认文件:[%s]存在且配置的用户有权限写, errorMessage:%s",
                    filePath, filePath, e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.OPEN_FILE_ERROR, message);
        }
    }



    @Override
    public String getRemoteFileContent(String filePath) {
        try {
            this.completePendingCommand();
            this.printWorkingDirectory();
            String parentDir = filePath.substring(0,
                    StringUtils.lastIndexOf(filePath, IOUtils.DIR_SEPARATOR));
            this.ftpClient.changeWorkingDirectory(new String(parentDir.getBytes("GBK"),FTP.DEFAULT_CONTROL_ENCODING));
            this.printWorkingDirectory();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(22);
            this.ftpClient.retrieveFile(new String(filePath.getBytes("GBK"),FTP.DEFAULT_CONTROL_ENCODING), outputStream);
            String result = outputStream.toString();
            IOUtils.closeQuietly(outputStream);
            return result;
        } catch (IOException e) {
            String message = String.format(
                    "读取文件 : [%s] 时出错,请确认文件:[%s]存在且配置的用户有权限读取, errorMessage:%s",
                    filePath, filePath, e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.OPEN_FILE_ERROR, message);
        }
    }

    @Override
    public Set<String> getAllFilesInDir(String dir, String prefixFileName) {
        Set<String> allFilesWithPointedPrefix = new HashSet<String>();
        try {

            // 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码（GBK）.
            if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {
                LOCAL_CHARSET = "UTF-8";
                //LOG.info("系统格式是=====================================================UTF-8");
            }
            ftpClient.setControlEncoding(LOCAL_CHARSET);
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            String ftpPath2 = new String(dir.getBytes(LOCAL_CHARSET),FTPClient.DEFAULT_CONTROL_ENCODING);

//            LOG.info("LOCAL_CHARSET---------------------------------"+LOCAL_CHARSET);
//
//            LOG.info("ftpPath2---------------------------------"+ftpPath2);

            boolean isDirExist = this.ftpClient.changeWorkingDirectory(ftpPath2);
            if (!isDirExist) {
                throw DataXException.asDataXException(
                        FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION,
                        String.format("进入目录[%s]失败", dir));
            }
            this.printWorkingDirectory();
            FTPFile[] fs = this.ftpClient.listFiles(ftpPath2);
            // LOG.debug(JSON.toJSONString(this.ftpClient.listNames(dir)));
            LOG.debug(String.format("ls: %s",
                    JSON.toJSONString(fs, SerializerFeature.UseSingleQuotes)));
            for (FTPFile ff : fs) {
                String strName = ff.getName();
                if (strName.startsWith(prefixFileName)) {
                    allFilesWithPointedPrefix.add(strName);
                }
            }
        } catch (IOException e) {
            String message = String
                    .format("获取path:[%s] 下文件列表时发生I/O异常,请确认与ftp服务器的连接正常,拥有目录ls权限, errorMessage:%s",
                            dir, e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
        }
        return allFilesWithPointedPrefix;
    }

    @Override
    public void deleteFiles(Set<String> filesToDelete) {
        String eachFile = null;
        boolean deleteOk = false;
        try {
            this.printWorkingDirectory();
            for (String each : filesToDelete) {
                LOG.info(String.format("delete file [%s].", each));
                eachFile = each;
                deleteOk = this.ftpClient.deleteFile(new String(each.getBytes("utf-8"),"iso-8859-1"));
                if (!deleteOk) {
                    String message = String.format(
                            "删除文件:[%s] 时失败,请确认指定文件有删除权限", eachFile);
                    throw DataXException.asDataXException(
                            FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION,
                            message);
                }
            }
        } catch (IOException e) {
            String message = String.format(
                    "删除文件:[%s] 时发生异常,请确认指定文件有删除权限,以及网络交互正常, errorMessage:%s",
                    eachFile, e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
        }
    }

    private void printWorkingDirectory() {
        try {
            LOG.info(String.format("current working directory:%s",
                    this.ftpClient.printWorkingDirectory()));
        } catch (Exception e) {
            LOG.warn(String.format("printWorkingDirectory error:%s",
                    e.getMessage()));
        }
    }

    @Override
    public void completePendingCommand() {
        /*
         * Q:After I perform a file transfer to the server,
         * printWorkingDirectory() returns null. A:You need to call
         * completePendingCommand() after transferring the file. wiki:
         * http://wiki.apache.org/commons/Net/FrequentlyAskedQuestions
         */
        try {
            boolean isOk = this.ftpClient.completePendingCommand();
            if (!isOk) {
                throw DataXException.asDataXException(
                        FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION,
                        "完成ftp completePendingCommand操作发生异常");
            }
        } catch (IOException e) {
            String message = String.format(
                    "完成ftp completePendingCommand操作发生异常, errorMessage:%s",
                    e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
        }
    }
}
