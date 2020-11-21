package com.github.gscaparrotti.bendermobile.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class OrderDto {
    @Expose
    @SerializedName("id")
    private long id;
    @Expose
    @SerializedName("dish")
    private DishDto dishDto;
    @Expose
    @SerializedName("customer")
    private CustomerDto customerDto;
    @Expose
    @SerializedName("time")
    private long time;
    @Expose
    @SerializedName("amount")
    private int amount;
    @Expose
    @SerializedName("served")
    private boolean served;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public DishDto getDish() {
        return dishDto;
    }

    public void setDish(DishDto dishDto) {
        this.dishDto = dishDto;
    }

    public CustomerDto getCustomer() {
        return customerDto;
    }

    public void setCustomer(CustomerDto customerDto) {
        this.customerDto = customerDto;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public boolean isServed() {
        return served;
    }

    public void setServed(boolean served) {
        this.served = served;
    }
}
