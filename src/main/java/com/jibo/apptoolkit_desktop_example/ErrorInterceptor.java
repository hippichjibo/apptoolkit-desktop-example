package com.jibo.apptoolkit_desktop_example;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.Interceptor;
import okio.Buffer;
import okio.BufferedSource;

// intercept requests and print out any errors for demonstration / debugging purposes 
class ErrorInterceptor implements Interceptor {
    @Override
    public okhttp3.Response intercept(Chain chain) throws IOException {
        okhttp3.Request request = chain.request();
        okhttp3.Response response = chain.proceed(request);

        if (response.code() >= 400) {
            try {
                BufferedSource source = response.body().source();
                source.request(Long.MAX_VALUE);
                Buffer buffer = source.buffer();
                String errorJsonBody = buffer.clone().readString(Charset.forName("UTF-8"));
                System.out.println("Error Interceptor: " + errorJsonBody);
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
            }
        }

        return response;
    }
};