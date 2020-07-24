package com.app.filedemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private final static String[] PERMISSIONS_STORAGE = {

            "android.permission.READ_EXTERNAL_STORAGE",

            "android.permission.WRITE_EXTERNAL_STORAGE"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);
        InputStream inputStream = getResources().openRawResource(R.raw.ftp_config);
        FtpServerManager.getInstance().startFtpServer(inputStream);
//        List<File> mVideoFileList = new ArrayList<>();
//        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ftp";
//        File ftpDirectory = new File(path);
//        if (ftpDirectory.exists() && ftpDirectory.isDirectory()) {
//            File[] files = ftpDirectory.listFiles();
//            for (File file : files) {
//
//                if (file.getName().endsWith("mp4")) {
//                    mVideoFileList.add(file);
//                }
//            }
//        }

    }


    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
