package com.example.program_kasir.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DetailRiwayatResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private TransaksiRiwayat data;

    @SerializedName("items")
    private List<ItemDetailRiwayat> items;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public TransaksiRiwayat getData() { return data; }
    public List<ItemDetailRiwayat> getItems() { return items; }
}
