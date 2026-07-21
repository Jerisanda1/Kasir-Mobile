package com.example.program_kasir.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TransaksiRequest {
    @SerializedName("cart")
    private List<CartItem> cart;

    @SerializedName("total")
    private double total;

    @SerializedName("diskon")
    private double diskon;

    @SerializedName("discount")
    private double discount;

    @SerializedName("nilai_diskon")
    private double nilaiDiskon;

    @SerializedName("bayar")
    private double bayar;

    @SerializedName("kembalian")
    private double kembalian;

    public TransaksiRequest(List<CartItem> cart, double total, double diskon, double bayar, double kembalian) {
        this.cart = cart;
        this.total = total;
        this.diskon = diskon;
        this.discount = diskon;
        this.nilaiDiskon = diskon;
        this.bayar = bayar;
        this.kembalian = kembalian;
    }
}