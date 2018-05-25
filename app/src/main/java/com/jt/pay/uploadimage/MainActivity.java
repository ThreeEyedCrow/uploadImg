package com.jt.pay.uploadimage;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String FILE_NAME = "imgTestZip.zip";
    private static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + FILE_NAME;
    //    String url = "http://120.79.47.183:8040/a/file/receiveApp/receiveZip";
    String url = "http://120.79.158.163:8040/a/file/receiveApp/receiveZip";
    private String fileUrl = "http://120.79.158.163:8040/";
    File file = new File(FILE_PATH);
    private static final int FILE_SIZE = 1024 * 1024 * 3; //块的大小为2M
    private boolean hasNext = true;

    private ProgressDialog dialog;
    private EditText editText;
    private TextView textView;
    private TextView addCode;
    private TextView uploadStatus;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        relativeLayout = findViewById(R.id.container);

        editText = findViewById(R.id.wechatNum);
        textView = findViewById(R.id.packageText);
        addCode = findViewById(R.id.addCode);
        uploadStatus = findViewById(R.id.uploadStatus);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        zipAll();
                    }
                }).start();
            }
        });

        addCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        uploadStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBackgroundColor();
            }
        });
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
                requestNeedFile();
            }
        });
    }

    private void zipAll() {
        String wechatPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tencent/MicroMsg/WeChat/"; //微信图片目录
//        String wechatPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tencent/MicroMsg/WeiXin/"; //微信图片目录
        File rootFile = new File(wechatPath);
        if (!rootFile.exists()) {
            Toast.makeText(MainActivity.this, "没有找到路径,请检查地址是否正确", Toast.LENGTH_SHORT).show();
            return;
        }
        File[] files = rootFile.listFiles(); // 得到f文件夹下面的所有文件。
        List<File> fileList = new ArrayList<File>();
        if (files == null || files.length == 0) {
            Toast.makeText(MainActivity.this, "文件夹下没有文件", Toast.LENGTH_SHORT).show();
            return;
        }
        for (File file : files) {
            if (!file.isDirectory()) {
                fileList.add(file);
            }
        }

        byte[] buf = new byte[1024];
        try {
            //ZipOutputStream类：完成文件或文件夹的压缩
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
            for (int i = 0; i < fileList.size(); i++) {
                FileInputStream in = new FileInputStream(fileList.get(i));
                out.putNextEntry(new ZipEntry(fileList.get(i).getName()));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    relativeLayout.setBackgroundColor(Color.parseColor("#ffff38"));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null && imageUris.size() > 0) {//file:///storage/emulated/0/tencent/MicroMsg/WeChat/mm_facetoface_collect_qrcode_1525573969969.png
            try {
                downLoadZIP(imageUris);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 打包成zip
     *
     * @param imageUris 文件的来源地址，字符串数组
     * @throws IOException
     */
    public void downLoadZIP(ArrayList<Uri> imageUris) throws IOException {
        if (!file.exists()) {
            file = File.createTempFile("imgTestZip", ".zip", Environment.getExternalStorageDirectory());
        }
        //zip输出流
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
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

    private int getFileBlockNum() {
        int num = (int) (file.length() / FILE_SIZE);
        if (file.length() % FILE_SIZE == 0) {
            return num;
        } else {
            return num + 1;
        }
    }

    private void requestNeedFile() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(fileUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GetFileListBiz getFileListBiz = retrofit.create(GetFileListBiz.class);
        Call<List<String>> call = getFileListBiz.getFileList(editText.getText().toString(), String.valueOf(getFileBlockNum()));
        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.body() == null) {
                    return;
                }
                List<String> fileList = response.body();
                if (fileList.size() > 0) {
                    splitFile(FILE_PATH, fileList);
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "请求文件列表失败" + call.toString() + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setBackgroundColor() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(fileUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GetFileListBiz getFileListBiz = retrofit.create(GetFileListBiz.class);
        Call<List<String>> call = getFileListBiz.getFileList(editText.getText().toString(), String.valueOf(getFileBlockNum()));
        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.body() == null) {
                    return;
                }
                List<String> fileList = response.body();
                if (fileList.size() == 0) {
                    relativeLayout.setBackgroundColor(Color.parseColor("#aaffff"));
                } else {
                    relativeLayout.setBackgroundColor(Color.parseColor("#ffffdd"));
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "请求文件列表失败" + call.toString() + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 封装后的单文件上传方法
     */
    public void upload(File file) {
        dialog.show();
        RetrofitClient
                .getInstance()
                .upLoadFile(url, file, editText.getText().toString(), new FileUploadObserver<ResponseBody>() {
                    @Override
                    public void onUpLoadSuccess(ResponseBody responseBody) {
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

    private void splitFile(String url, List<String> stringList) {
        try {
            FileInputStream fin = new FileInputStream(url);
            FileChannel fcin = fin.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(FILE_SIZE);
            int count = 1;
            while (true) {
                buffer.clear();
                int flag = fcin.read(buffer);
                if (flag == -1) {
                    break;
                }
                buffer.flip();
                FileOutputStream fout = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + count + ".print");
                FileChannel fcout = fout.getChannel();
                fcout.write(buffer);
                for (int i = 0; i < stringList.size(); i++) {
                    if (Integer.parseInt(stringList.get(i)) == count) {
                        upload(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + count + ".print"));
                    }
                }
                count++;
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

//    public void multiUpload() {
//        dialog.show();
//        RetrofitClient
//                .getInstance()
//                .multiUpLoadFile(url, file,editText.getText().toString(), new FileUploadObserver<ResponseBody>() {
//                    @Override
//                    public void onUpLoadSuccess(ResponseBody responseBody) {
//                        Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
//                        relativeLayout.setBackgroundColor(Color.parseColor("#aaffff"));
//                        dialog.dismiss();
//                    }
//
//                    @Override
//                    public void onUpLoadFail(Throwable e) {
//                        Toast.makeText(MainActivity.this, "上传失败" + e.getMessage(), Toast.LENGTH_SHORT).show();
//                        relativeLayout.setBackgroundColor(Color.parseColor("#ffffdd"));
//                        dialog.dismiss();
//                    }
//
//                    @Override
//                    public void onProgress(int progress) {
//                        dialog.setProgress(progress);
//                    }
//                });
//    }


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
