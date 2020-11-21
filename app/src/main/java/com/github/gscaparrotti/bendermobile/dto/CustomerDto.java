package com.github.gscaparrotti.bendermobile.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CustomerDto {
    @Expose
    @SerializedName("table")
    private TableDto tableDto;
    @Expose
    @SerializedName("workingTable")
    private TableDto workingTableDto;
    @Expose
    @SerializedName("name")
    private String name;

    public TableDto getTable() {
        return tableDto;
    }

    public void setTable(TableDto tableDto) {
        this.tableDto = tableDto;
    }

    public TableDto getWorkingTable() {
        return workingTableDto;
    }

    public void setWorkingTable(TableDto workingTableDto) {
        this.workingTableDto = workingTableDto;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
