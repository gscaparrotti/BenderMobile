package com.github.gscaparrotti.bendermobile.dto;

import com.github.gscaparrotti.bendermobile.network.HttpServerInteractor;
import com.github.gscaparrotti.bendermobile.network.PendingHttpRequest;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.util.List;

public class DishDto {
    @Expose
    @SerializedName("filter")
    private int filter;
    @Expose
    @SerializedName("temporary")
    private boolean temporary;
    @Expose
    @SerializedName("price")
    private double price;
    @Expose
    @SerializedName("name")
    private String name;
    @Expose
    @SerializedName("@type")
    private String type;

    public int getFilter() {
        return filter;
    }

    public void setFilter(int filter) {
        this.filter = filter;
    }

    public boolean getTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static PendingHttpRequest getGetMenuRequest() {
        return new PendingHttpRequest()
            .setMethod(HttpServerInteractor.Method.GET)
            .setEndpoint("menu")
            .setReturnType(new TypeToken<List<DishDto>>(){}.getType());
    }
}
