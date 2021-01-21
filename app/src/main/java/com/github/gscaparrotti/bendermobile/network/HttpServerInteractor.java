package com.github.gscaparrotti.bendermobile.network;

import com.github.gscaparrotti.bendermobile.R;
import com.github.gscaparrotti.bendermobile.activities.MainActivity;
import com.github.gscaparrotti.bendermodel.model.Pair;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpServerInteractor {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static HttpServerInteractor singleton;
    private final OkHttpClient okHttpClient;
    private final Gson gson;

    public static HttpServerInteractor getInstance() {
        if (singleton == null) {
            singleton = new HttpServerInteractor();
        }
        return singleton;
    }

    private HttpServerInteractor() {
        this.okHttpClient = new OkHttpClient.Builder().connectTimeout(5000, TimeUnit.MILLISECONDS).build();
        this.gson = new Gson();
    }

    public final <T> T newSendAndReceive(final PendingHttpRequest pendingHttpRequest) {
        final HttpUrl.Builder httpUrlBuilder = new HttpUrl.Builder()
            .scheme("http")
            .host(pendingHttpRequest.getAddress())
            .port(pendingHttpRequest.getPort())
            .addPathSegment("api")
            .addPathSegment(pendingHttpRequest.getEndpoint());
        for (final Pair<String, String> queryParam : pendingHttpRequest.getQueryParams()) {
            httpUrlBuilder.addQueryParameter(queryParam.getX(), queryParam.getY());
        }
        final Request.Builder requestBuilder = new Request.Builder().url(httpUrlBuilder.build());
        for (final Pair<String, String> headerParam : pendingHttpRequest.getHeaderParams()) {
            requestBuilder.header(headerParam.getX(), headerParam.getY());
        }
        if (pendingHttpRequest.getMethod() == Method.POST) {
            if (pendingHttpRequest.getJson() != null) {
                final RequestBody requestBody = RequestBody.create(pendingHttpRequest.getJson(), JSON);
                requestBuilder.post(requestBody);
            } else {
                throw new IllegalArgumentException("Body must be non-null with POST method");
            }
        } else if (pendingHttpRequest.getMethod() == Method.DELETE) {
            if (pendingHttpRequest.getJson() != null) {
                final RequestBody requestBody = RequestBody.create(pendingHttpRequest.getJson(), JSON);
                requestBuilder.delete(requestBody);
            } else {
                requestBuilder.delete();
            }
        }
        try (final Response response = okHttpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new BenderNetworkException(MainActivity.commonContext.getString(R.string.ErroreHTTP) + ": " + response.code() + " " + response.message());
            }
            final String responseString = response.body() != null ? Objects.requireNonNull(response.body()).string() : "";
            if (responseString.length() > 0 || pendingHttpRequest.getReturnType() == null) {
                return gson.fromJson(responseString, pendingHttpRequest.getReturnType());
            }
            throw new BenderNetworkException(MainActivity.commonContext.getString(R.string.ErroreHTTP) +": empty response.");
        } catch (final IOException e) {
            throw new BenderNetworkException(MainActivity.commonContext.getString(R.string.ErroreHTTP) +": " + e.getMessage());
        }
    }

    public enum Method {
        GET, POST, DELETE
    }
}
