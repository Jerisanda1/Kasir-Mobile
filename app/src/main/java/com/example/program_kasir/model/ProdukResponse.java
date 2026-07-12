package com.example.program_kasir.model;

import com.example.program_kasir.Produk;
import java.util.List;

public class ProdukResponse {
    private boolean success;
    private List<Produk> data;

    public boolean isSuccess() { return success; }
    public List<Produk> getData() { return data; }
}