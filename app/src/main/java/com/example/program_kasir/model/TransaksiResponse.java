package com.example.program_kasir.model;

public class TransaksiResponse {
    private boolean success;
    private String message;
    private String kode_transaksi;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getKodeTransaksi() { return kode_transaksi; }
}