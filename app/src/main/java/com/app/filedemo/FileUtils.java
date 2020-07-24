package com.app.filedemo;

import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtils {

    private final static int SECTION_SIZE = 1024 * 1024; //1M


    public static void createTempFile() {
        try {
            //VID_20200721_104827.mp4
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/video/test.txt";
            String copyPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/video/VID_COPY.mp4";
            RandomAccessFile randomAccessFile = new RandomAccessFile(path, "r");
            String sha = encryptToSHA(fileToBytes(randomAccessFile));
            Log.d("wjq", "sha = " + sha);
            long fileLength = randomAccessFile.length();
            BigDecimal fileDecimal = new BigDecimal(fileLength);
            BigDecimal sectionDecimal = new BigDecimal(SECTION_SIZE);
            int sectionCount = fileDecimal.divide(sectionDecimal, BigDecimal.ROUND_UP).intValue();
            Log.d("wjq", "fileLength = " + fileLength + " | sectionCount = " + sectionCount);
            long offset = 0L;
            for (int i = 0; i < sectionCount; i++) {
                long sectionBegin = offset;
                long sectionEnd;
                if (i <= sectionCount - 1) {
                    sectionEnd = (i + 1) * SECTION_SIZE;
                } else {
                    sectionEnd = fileLength;
                }
                offset = writeSection(path, i, sectionBegin, sectionEnd);

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //读文件
    public static void readSection(String tempFile, int tempIndex) {
        String content = null;
        try {
            //开始合并文件，对应切片的二进制文件

            //读取切片文件
            RandomAccessFile reader = new RandomAccessFile(new File(tempFile + "_" + tempIndex + ".tmp"), "r");
            byte[] b = new byte[1024];
            int n;
            while ((n = reader.read(b)) != -1) {
                content += new String(b, "utf-8");
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Log.d("wjq", content);
        }
    }

    //写文件
    public static long writeSection(String file, int index, long sectionBegin, long sectionEnd) {
        long endPointer = 0L;
        try {
            RandomAccessFile in = new RandomAccessFile(new File(file), "r");
            RandomAccessFile out = new RandomAccessFile(new File(file + "_" + index + ".tmp"), "rw");
            byte[] buffer = new byte[1024];
            int n;

            in.seek(sectionBegin);
            while (in.getFilePointer() <= sectionEnd && (n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            endPointer = in.getFilePointer();
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return endPointer;
    }

    //文件合并
    public static void merge(String file, String tempFile, int tempCount) {
        RandomAccessFile raf = null;
        try {
            //申明随机读取文件RandomAccessFile
            raf = new RandomAccessFile(new File(file), "rw");
            //开始合并文件，对应切片的二进制文件
            for (int i = 0; i < tempCount; i++) {
                //读取切片文件
                RandomAccessFile reader = new RandomAccessFile(new File(tempFile + "_" + i + ".tmp"), "r");
                byte[] b = new byte[1024];
                int n;
                while ((n = reader.read(b)) != -1) {
                    raf.write(b, 0, n);//一边读，一边写
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //文件转byte数组
    public static byte[] fileToBytes(RandomAccessFile randomAccessFile) {
        byte[] buffer = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            byte[] b = new byte[1024];
            int n;
            while ((n = randomAccessFile.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            randomAccessFile.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;

    }

    //byte数组sha1加密
    public static String encryptToSHA(byte[] bytes) {
        byte[] digesta = null;
        try {
            MessageDigest alga = MessageDigest.getInstance("SHA-1");
            alga.update(bytes);
            digesta = alga.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String rs = byte2hex(digesta);
        return rs;
    }

    //sha1加密字符串
    public static String byte2hex(byte[] b) {
        String hs = "";
        String sTmp = "";
        for (int n = 0; n < b.length; n++) {
            sTmp = (Integer.toHexString(b[n] & 0XFF));
            if (sTmp.length() == 1) {
                hs = hs + "0" + sTmp;
            } else {
                hs = hs + sTmp;
            }
        }
        return hs;
    }
}
