package com.jt.pay.uploadimage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * 扩展OkHttp的请求体，实现上传时的进度提示
 */
public class UploadFileRequestBody extends RequestBody {

    private RequestBody mRequestBody;
    private FileUploadObserver<ResponseBody> fileUploadObserver;
    private File file;

    public UploadFileRequestBody(File file, FileUploadObserver<ResponseBody> fileUploadObserver) {
        this.mRequestBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        this.fileUploadObserver = fileUploadObserver;
        this.file = file;
    }

    @Override
    public MediaType contentType() {
        return mRequestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return mRequestBody.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        int length;
        byte[] buffer = new byte[1024 * 1024];
        while ((length = fileInputStream.read(buffer)) != -1) {
            sink.write(buffer, 0, length);
        }


//        CountingSink countingSink = new CountingSink(sink);
//        BufferedSink bufferedSink = Okio.buffer(countingSink);
//        //写入
//        mRequestBody.writeTo(bufferedSink);
//        //刷新
//        //必须调用flush，否则最后一部分数据可能不会被写入
//        bufferedSink.flush();
    }

    protected final class CountingSink extends ForwardingSink {

        private long bytesWritten = 0;

        public CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);

            bytesWritten += byteCount;
            if (fileUploadObserver != null) {
                fileUploadObserver.onProgressChange(bytesWritten, contentLength());
            }
        }
    }
}