package com.example.program_kasir.model;

import com.google.gson.annotations.SerializedName;

public class ItemDetailRiwayat {

    @SerializedName("nama_produk")
    private String namaProduk;

    @SerializedName("qty")
    private int qty;

    @SerializedName("harga")
    private double harga;

    @SerializedName("subtotal")
    private double subtotal;

    public String getNamaProduk() { return namaProduk; }
    public int getQty() { return qty; }
    public double getHarga() { return harga; }
    public double getSubtotal() { return subtotal; }
}
