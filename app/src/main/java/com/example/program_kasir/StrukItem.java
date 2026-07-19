package com.example.program_kasir;

public class StrukItem {
    public String namaProduk;
    public int qty;
    public double harga;
    public double subtotal;

    public StrukItem(String namaProduk, int qty, double harga, double subtotal) {
        this.namaProduk = namaProduk;
        this.qty = qty;
        this.harga = harga;
        this.subtotal = subtotal;
    }
}
