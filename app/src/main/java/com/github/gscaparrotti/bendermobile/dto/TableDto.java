package com.github.gscaparrotti.bendermobile.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

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
}
