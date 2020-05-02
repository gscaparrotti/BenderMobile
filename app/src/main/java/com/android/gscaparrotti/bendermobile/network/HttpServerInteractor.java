package com.android.gscaparrotti.bendermobile.network;

import com.android.gscaparrotti.bendermobile.R;
import com.android.gscaparrotti.bendermobile.activities.MainActivity;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpServerInteractor {

    private static HttpServerInteractor singleton;
    private final OkHttpClient okHttpClient;

    public static HttpServerInteractor getInstance() {
        if (singleton == null) {
            singleton = new HttpServerInteractor();
        }
        return singleton;
    }

    private HttpServerInteractor() {
        this.okHttpClient = new OkHttpClient();
    }

    public String get(final String address, final int port, final String endpoint) {
        Request request = new Request.Builder()
            .url(address + ":" + port + "/" + endpoint)
            .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.body().string();
        } catch (IOException e) {
            throw new BenderNetworkException(MainActivity.commonContext.getString(R.string.ErroreSocketConChiusura));
        }
    }

}
