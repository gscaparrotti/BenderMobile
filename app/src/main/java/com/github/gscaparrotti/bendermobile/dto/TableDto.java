package com.github.gscaparrotti.bendermobile.dto;

import com.github.gscaparrotti.bendermobile.network.HttpServerInteractor;
import com.github.gscaparrotti.bendermobile.network.PendingHttpRequest;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.util.List;

public class TableDto {
    @Expose
    @SerializedName("tableNumber")
    private int tableNumber;

    public int getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
    }

    public static PendingHttpRequest getGetTableDtoRequest() {
        return new PendingHttpRequest()
            .setMethod(HttpServerInteractor.Method.GET)
            .setEndpoint("tables")
            .setReturnType(new TypeToken<List<TableDto>>(){}.getType());
    }
}
