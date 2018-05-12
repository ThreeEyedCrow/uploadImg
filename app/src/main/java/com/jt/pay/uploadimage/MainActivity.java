package com.jt.pay.uploadimage;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog dialog;
    String url = "http://192.168.191.1:8080/UploadFileServer/servlet/UploadHandleServlet";
    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "imgTestZip.zip");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        handleSendMultipleImages(intent); // Handle multiple images being sent
                    }
                }).start();
            }
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog = new ProgressDialog(MainActivity.this);
                dialog.setMax(100);
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.setMessage("上传文件中");

                upload();
            }
        });
    }

    private void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null && imageUris.size() > 0) {
//            for (int i = 0; i < imageUris.size(); i++){
//                try {
//                    FileInputStream fileInputStream = new FileInputStream(imageUris.get(i).getPath());
//                    Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }

            try {
                downLoadZIP(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "imgTestZip.zip", imageUris);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 打包成zip
     *
     * @param tagPath   zip的输出地址
     * @param imageUris 文件的来源地址，字符串数组
     * @throws IOException
     */
    public void downLoadZIP(String tagPath, ArrayList<Uri> imageUris) throws IOException {
        //zip输出流
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tagPath));
        File[] files = new File[imageUris.size()];
        //按照多个文件的打包方式，一个也可以
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(imageUris.get(i).getPath());
        }
        byte[] b = new byte[1024];
        for (int j = 0; j < files.length; j++) {
            //输入流
            FileInputStream in = new FileInputStream(files[j]);
            //把条目放到zip里面，意思就是把文件放到压缩文件里面
            out.putNextEntry(new ZipEntry(files[j].getName()));
            int len = 0;
            //输出
            while ((len = in.read(b)) > -1) {
                out.write(b, 0, len);
            }
            out.closeEntry();
            in.close();
        }
        out.close();
    }

    /**
     * 封装后的单文件上传方法
     */
    public void upload() {
        dialog.show();
        RetrofitClient
                .getInstance()
                .upLoadFile(url, file, new FileUploadObserver<ResponseBody>() {
                    @Override
                    public void onUpLoadSuccess(ResponseBody responseBody) {
                        Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onUpLoadFail(Throwable e) {
                        Toast.makeText(MainActivity.this, "上传失败" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onProgress(int progress) {
                        dialog.setProgress(progress);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
