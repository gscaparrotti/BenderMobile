package com.android.gscaparrotti.bendermobile.network;

import com.android.gscaparrotti.bendermobile.R;
import com.android.gscaparrotti.bendermobile.activities.MainActivity;
import com.github.gscaparrotti.bendermodel.model.Pair;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpServerInteractor {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static HttpServerInteractor singleton;
    private final OkHttpClient okHttpClient;

    public static HttpServerInteractor getInstance() {
        if (singleton == null) {
            singleton = new HttpServerInteractor();
        }
        return singleton;
    }

    private HttpServerInteractor() {
        this.okHttpClient = new OkHttpClient.Builder().connectTimeout(5000, TimeUnit.MILLISECONDS).build();
    }

    @SafeVarargs
    public final String sendAndReceiveAsString(final String address, final int port, final String endpoint, final Method method, final String body, final Pair<String, String>... params) {
        final Request.Builder requestBuilder = new Request.Builder()
            .url("http://"+ address + ":" + port + "/api/" + endpoint);
        for (final Pair<String, String> param : params) {
            requestBuilder.header(param.getX(), param.getY());
        }
        if (method == Method.POST) {
            if (body != null) {
                final RequestBody requestBody = RequestBody.create(body, JSON);
                requestBuilder.post(requestBody);
            } else {
                throw new IllegalArgumentException("Body must be non-null with POST method");
            }
        } else if (method == Method.DELETE) {
            if (body != null) {
                final RequestBody requestBody = RequestBody.create(body, JSON);
                requestBuilder.delete(requestBody);
            } else {
                requestBuilder.delete();
            }
        }
        try (final Response response = okHttpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new BenderNetworkException(MainActivity.commonContext.getString(R.string.ErroreHTTP) + ": " + response.code());
            }
            return response.body() != null ? Objects.requireNonNull(response.body()).string() : "";
        } catch (final IOException e) {
            throw new BenderNetworkException(MainActivity.commonContext.getString(R.string.ErroreHTTP));
        }
    }

    @SafeVarargs
    public final JsonArray sendAndReceiveAsJsonArray(final String address, final int port, final String endpoint, final Method method, final String body, final Pair<String, String>... params) {
        return new Gson().fromJson(sendAndReceiveAsString(address, port, endpoint, method, body, params), JsonArray.class);
    }

    @SafeVarargs
    public final JsonObject sendAndReceiveAsJsonObject(final String address, final int port, final String endpoint, final Method method, final String body, final Pair<String, String>... params) {
        return new Gson().fromJson(sendAndReceiveAsString(address, port, endpoint, method, body, params), JsonObject.class);
    }

    public enum Method {
        GET, POST, DELETE
    }

}
