package com.github.gscaparrotti.bendermobile.network;

import com.github.gscaparrotti.bendermobile.R;
import com.github.gscaparrotti.bendermobile.activities.MainActivity;
import com.github.gscaparrotti.bendermodel.model.Pair;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
        String url = null;
        try {
            final String[] splitEndpoint = endpoint.split("\\?", 2);
            final URI uri = new URI("http",  null, address, port, "/api/" + splitEndpoint[0], splitEndpoint.length > 1 ? splitEndpoint[1] : null, null);
            url = uri.toURL().toString();
        } catch (final URISyntaxException | MalformedURLException e) {
            //fallback
            url = "http://"+ address + ":" + port + "/api/" + endpoint;
        }
        final Request.Builder requestBuilder = new Request.Builder().url(url);
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
                throw new BenderNetworkException(MainActivity.commonContext.getString(R.string.ErroreHTTP) + ": " + response.code() + " " + response.message());
            }
            return response.body() != null ? Objects.requireNonNull(response.body()).string() : "";
        } catch (final IOException e) {
            throw new BenderNetworkException(MainActivity.commonContext.getString(R.string.ErroreHTTP) +": " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    @SafeVarargs
    public final JsonArray sendAndReceiveAsJsonArray(final String address, final int port, final String endpoint, final Method method, final String body, final Pair<String, String>... params) {
        return new Gson().fromJson(sendAndReceiveAsString(address, port, endpoint, method, body, params), JsonArray.class);
    }

    @SuppressWarnings("unused")
    @SafeVarargs
    public final JsonObject sendAndReceiveAsJsonObject(final String address, final int port, final String endpoint, final Method method, final String body, final Pair<String, String>... params) {
        return new Gson().fromJson(sendAndReceiveAsString(address, port, endpoint, method, body, params), JsonObject.class);
    }

    @SafeVarargs
    public final <T> T sendAndReceive(final Class<T> clazz, final String address, final int port, final String endpoint, final Method method, final String body, final Pair<String, String>... params) {
        return new Gson().fromJson(sendAndReceiveAsString(address, port, endpoint, method, body, params), clazz);
    }

    public enum Method {
        GET, POST, DELETE
    }

}
