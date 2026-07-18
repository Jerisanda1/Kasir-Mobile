package com.example.program_kasir.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RiwayatResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<TransaksiRiwayat> data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<TransaksiRiwayat> getData() { return data; }
}
