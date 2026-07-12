package com.example.program_kasir.model;

public class CartItem {
    private int kode_produk;
    private int qty;
    private double harga;
    private double subtotal;

    public CartItem(int kodeProduk, int qty, double harga) {
        this.kode_produk = kodeProduk;
        this.qty = qty;
        this.harga = harga;
        this.subtotal = qty * harga;
    }
}