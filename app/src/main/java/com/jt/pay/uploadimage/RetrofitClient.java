package com.jt.pay.uploadimage;


import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import java.io.File;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Url;

public class RetrofitClient {
    private static RetrofitClient mInstance;
    private static Retrofit retrofit;

    private RetrofitClient() {
        retrofit = new Retrofit.Builder()
                .baseUrl(Api.BASE_URL)
                .client(OkHttpManager.getInstance())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

    }

    static RetrofitClient getInstance() {
        if (mInstance == null) {
            synchronized (RetrofitClient.class) {
                if (mInstance == null) {
                    mInstance = new RetrofitClient();
                }
            }
        }
        return mInstance;
    }

    private <T> T create(Class<T> clz) {
        return retrofit.create(clz);
    }

    Api api() {
        return RetrofitClient.getInstance().create(Api.class);
    }

    /**
     * 单上传文件的封装
     *
     * @param url                完整的接口地址
     * @param file               需要上传的文件
     * @param fileUploadObserver 上传回调
     */
    void upLoadFile(String url, File file, String wechatNum,FileUploadObserver<ResponseBody> fileUploadObserver) {
        UploadFileRequestBody uploadFileRequestBody = new UploadFileRequestBody(file, fileUploadObserver);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), uploadFileRequestBody);
        RequestBody wechatNumber = RequestBody.create(okhttp3.MediaType.parse("charset=utf-8"), wechatNum);
        create(UpLoadFileApi.class)
                .uploadFile(url,wechatNumber, part)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fileUploadObserver);
    }

//    /**
//     * 多上传文件的封装
//     *
//     * @param url                完整的接口地址
//     * @param fileList               需要上传的文件
//     * @param fileUploadObserver 上传回调
//     */
//    void multiUpLoadFile(String url, List<File> fileList, String wechatNum, FileUploadObserver<ResponseBody> fileUploadObserver) {
//        UploadFileRequestBody uploadFileRequestBody = new UploadFileRequestBody(fileList, fileUploadObserver);
//        MultipartBody.Part part = MultipartBody.Part.createFormData("file", fileList.getName(), uploadFileRequestBody);
//        RequestBody wechatNumber = RequestBody.create(okhttp3.MediaType.parse("charset=utf-8"), wechatNum);
//        create(UpLoadFileApi.class)
//                .uploadFile(url,wechatNumber, part)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(fileUploadObserver);
//    }

    //上传文件的interface
    interface UpLoadFileApi {
        @Multipart
        @POST
        Observable<ResponseBody> uploadFile(@Url String url, @Part("description") RequestBody description, @Part MultipartBody.Part file);
    }

    interface MultiUpLoadFileApi {
        @Multipart
        @POST
        Observable<ResponseBody> uploadFile(@Url String url, @Part("description") RequestBody description, @Part List<MultipartBody.Part> partList);
    }

}
