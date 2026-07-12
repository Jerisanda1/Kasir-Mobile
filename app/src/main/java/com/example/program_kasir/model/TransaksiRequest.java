package com.example.program_kasir.model;

import java.util.List;

public class TransaksiRequest {
    private List<CartItem> cart;
    private double total;
    private double bayar;
    private double kembalian;

    public TransaksiRequest(List<CartItem> cart, double total, double bayar, double kembalian) {
        this.cart = cart;
        this.total = total;
        this.bayar = bayar;
        this.kembalian = kembalian;
    }
}