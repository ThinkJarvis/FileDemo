package com.app.filedemo;

import android.os.Environment;
import android.util.Log;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FtpServerManager {

    private final static String TAG = "FtpServerManager";

    private static FtpServerManager sInstance;

    public final static String sentryDirPath = /*"/private/ftp"*/Environment.getExternalStorageDirectory().getAbsolutePath() + "/ftp";
    //ftp服务器配置文件路径
    private final static String fileNamePath = sentryDirPath + "/ftp_user.properties";

    private final static int PORT = 2221;

    private final static String HOST_IP = "172.16.101.21";

    private static FtpServer ftpServer = null;

    public static FtpServerManager getInstance() {
        if (sInstance == null) {
            synchronized (FtpServerManager.class) {
                if (sInstance == null) {
                    sInstance = new FtpServerManager();
                }
            }
        }
        return sInstance;
    }


    private FtpServerManager() {

    }


    public void startFtpServer(InputStream inputStream) {
        try {
            createDirsFiles(inputStream);
            FtpServerFactory serverFactory = new FtpServerFactory();
            PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
            File file = new File(fileNamePath);
            //设置配置文件
            userManagerFactory.setFile(file);
            serverFactory.setUserManager(userManagerFactory.createUserManager());


            // 设置监听IP和端口号
            ListenerFactory factory = new ListenerFactory();
            factory.setPort(PORT);
            factory.setServerAddress(getLocalIpAddress());
            serverFactory.addListener("default", factory.createListener());


            BaseUser user = new BaseUser();
            //设置权限
            List<Authority> authorities = new ArrayList<>();
            authorities.add(new WritePermission());
            user.setAuthorities(authorities);
            //设置用户名密码
            user.setName("admin");
            user.setPassword("123456");
            user.setEnabled(true);
            user.setMaxIdleTime(3000);
            user.setHomeDirectory(sentryDirPath);


            serverFactory.getUserManager().save(user);

            // 配置服务器被操作的命令等回复信息
            Map ftpLets = new HashMap<>();
            ftpLets.put("sentryLet", new SentryLet());
            serverFactory.setFtplets(ftpLets);

            stopServer();

            ftpServer = serverFactory.createServer();
            ftpServer.start();
            Log.d(TAG, "ftpServer start");
        } catch (FtpException e) {
            e.printStackTrace();
            Log.d(TAG, "ftpServer start e = " + e.toString());
        }
    }

    private void stopServer() {
        if (ftpServer != null) {
            ftpServer.stop();
            Log.d(TAG, "ftpServer stopServer");
        }
    }


    private void createDirsFiles(InputStream ins) {
        try {
            Log.d(TAG, "sentryDirPath = " + sentryDirPath);
            File sentry = new File(sentryDirPath);
            if (!sentry.exists()) {
                boolean result = sentry.mkdir();
                Log.d(TAG, "sourceFile = " + result);
            }

            File sourceFile = new File(fileNamePath);

            if (sourceFile.exists()) {
                sourceFile.delete();
            }

            FileOutputStream fos = new FileOutputStream(sourceFile);

            int n = 0;
            byte[] buffer = new byte[1024];
            while ((n = ins.read(buffer)) != -1) {
                fos.write(buffer, 0, n);
            }

            fos.close();
            ins.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface intf : interfaces) {
                Log.d(TAG, intf.toString());
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = isIpV4(sAddr);
                        if (isIPv4) {
                            Log.d(TAG, "sAddr =" + sAddr);
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static boolean isIpV4(String ipv4) {
        if (ipv4 == null || ipv4.length() == 0) {
            return false;//字符串为空或者空串
        }
        String[] parts = ipv4.split("\\.");//因为java doc里已经说明, split的参数是reg, 即正则表达式, 如果用"|"分割, 则需使用"\\|"
        if (parts.length != 4) {
            return false;//分割开的数组根本就不是4个数字
        }
        for (int i = 0; i < parts.length; i++) {
            try {
                int n = Integer.parseInt(parts[i]);
                if (n < 0 || n > 255) {
                    return false;//数字不在正确范围内
                }
            } catch (NumberFormatException e) {
                return false;//转换数字不正确
            }
        }
        return true;
    }


    private class SentryLet extends DefaultFtplet {
        //客户端发过来的命令。如登录命令，连接命令，切换目录命令，上传命令等
        @Override
        public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
            FtpletResult ftpletResult = super.beforeCommand(session, request);
            Log.d(TAG, "beforeCommand = " + session.getClientAddress() + " | " + request.getCommand());
            return ftpletResult;
        }

        //客户端命令执行之后的应答
        @Override
        public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException {
            FtpletResult ftpletResult = super.afterCommand(session, request, reply);
            Log.d(TAG, "afterCommand = " + session.getClientAddress() + " | " + request.getCommand() + " | " + reply.toString());
            return ftpletResult;
        }

        //上传开始的状态应答，客户端开启上传过程之前的最后一次应答
        @Override
        public FtpletResult onUploadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
            FtpletResult ftpletResult = super.onUploadStart(session, request);

            Log.d(TAG, "onUploadStart = " + session.getClientAddress() + " | " + session.getRenameFrom().getName());
            return ftpletResult;
        }

        //上传结束的状态应答，客户端结束上传过程的最后一次应答
        @Override
        public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
            FtpletResult ftpletResult = super.onUploadEnd(session, request);
            Log.d(TAG, "onUploadEnd = " + session.getClientAddress() + " | " + session.getRenameFrom().getName());
            return ftpletResult;
        }
    }
}

