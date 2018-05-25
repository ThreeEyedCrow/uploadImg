package com.jt.pay.uploadimage;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GetFileListBiz {
    @GET("a/file/receiveApp/lackPrintNum")
    Call<List<String>> getFileList(@Query("account") String account,
                                   @Query("wxSum") String wxSum);
}
