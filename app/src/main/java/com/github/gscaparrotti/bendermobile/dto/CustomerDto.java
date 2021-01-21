package com.github.gscaparrotti.bendermobile.dto;

import com.github.gscaparrotti.bendermobile.network.HttpServerInteractor;
import com.github.gscaparrotti.bendermobile.network.PendingHttpRequest;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.util.List;

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

    public static PendingHttpRequest getGetCustomerDtoRequest() {
        return new PendingHttpRequest()
            .setMethod(HttpServerInteractor.Method.GET)
            .setEndpoint("customers")
            .setReturnType(new TypeToken<List<CustomerDto>>(){}.getType());
    }

    public static PendingHttpRequest getGetCustomerDtoRequest(final int tableNumber) {
        return CustomerDto.getGetCustomerDtoRequest()
            .addQueryParam("tableNumber", Integer.toString(tableNumber));
    }

    public static PendingHttpRequest getUpdateCustomerRequest(final CustomerDto customerDto) {
        return new PendingHttpRequest()
            .setMethod(HttpServerInteractor.Method.POST)
            .setEndpoint("customers")
            .setJsonFromObject(customerDto);
    }
}
