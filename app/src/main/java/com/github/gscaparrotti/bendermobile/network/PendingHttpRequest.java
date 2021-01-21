package com.github.gscaparrotti.bendermobile.network;

import com.github.gscaparrotti.bendermobile.R;
import com.github.gscaparrotti.bendermobile.activities.MainActivity;
import com.github.gscaparrotti.bendermodel.model.Pair;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PendingHttpRequest {

    private static final Gson gson = new Gson();

    private String address;
    private Integer port;
    private String endpoint;
    private HttpServerInteractor.Method method;
    private String json;
    private List<Pair<String, String>> headerParams;
    private List<Pair<String, String>> queryParams;
    private Type returnType = Object.class;

    public PendingHttpRequest setAddress(final String address) {
        this.address = address;
        return this;
    }

    public PendingHttpRequest setPort(final int port) {
        this.port = port;
        return this;
    }

    public PendingHttpRequest setEndpoint(final String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public PendingHttpRequest setMethod(final HttpServerInteractor.Method method) {
        this.method = method;
        return this;
    }

    public PendingHttpRequest setJsonFromObject(final Object object) {
        this.json = gson.toJson(object);
        return this;
    }

    public PendingHttpRequest setJson(final String json) {
        this.json = json;
        return this;
    }

    public PendingHttpRequest addHeaderParam(final String key, final String value) {
        if (this.headerParams == null) {
            this.headerParams = new ArrayList<>();
        }
        this.headerParams.add(new Pair<>(key, value));
        return this;
    }

    public PendingHttpRequest addQueryParam(final String key, final String value) {
        if (this.queryParams == null) {
            this.queryParams = new ArrayList<>();
        }
        this.queryParams.add(new Pair<>(key, value));
        return this;
    }

    public PendingHttpRequest setReturnType(final Type returnType) {
        this.returnType = returnType;
        return this;
    }

    public String getAddress() {
        if (this.address != null) {
            return this.address;
        } else {
            return MainActivity.commonContext.getSharedPreferences("BenderIP", 0).getString("BenderIP", "Absent");
        }
    }

    public int getPort() {
        if (this.port != null) {
            return this.port;
        } else {
            return Integer.parseInt(MainActivity.commonContext.getResources().getString(R.string.serverPort));
        }
    }

    public String getEndpoint() {
        return this.endpoint;
    }

    public HttpServerInteractor.Method getMethod() {
        return this.method;
    }

    public String getJson() {
        return this.json;
    }

    public List<Pair<String, String>> getHeaderParams() {
        if (this.headerParams == null) {
            return Collections.emptyList();
        }
        return this.headerParams;
    }

    public List<Pair<String, String>> getQueryParams() {
        if (this.queryParams == null) {
            return Collections.emptyList();
        }
        return this.queryParams;
    }

    public Type getReturnType() {
        return this.returnType;
    }
}
