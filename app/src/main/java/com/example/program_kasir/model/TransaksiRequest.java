package com.example.program_kasir.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TransaksiRequest {
    private List<CartItem> cart;
    private double total;
    private double bayar;
    private double kembalian;

    @SerializedName("metode_pembayaran")
    private String metodePembayaran;

    public TransaksiRequest(List<CartItem> cart, double total, double bayar, double kembalian, String metodePembayaran) {
        this.cart = cart;
        this.total = total;
        this.bayar = bayar;
        this.kembalian = kembalian;
        this.metodePembayaran = metodePembayaran;
    }
}