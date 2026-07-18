package com.example.program_kasir.model;

import com.google.gson.annotations.SerializedName;

public class TransaksiRiwayat {

    @SerializedName("kode_transaksi")
    private String kodeTransaksi;

    @SerializedName("total")
    private double total;

    @SerializedName("bayar")
    private double bayar;

    @SerializedName("kembalian")
    private double kembalian;

    @SerializedName("shift")
    private String shift;

    @SerializedName("created_at")
    private String createdAt;

    // Hanya terisi kalau yang login Admin (hasil join ke tblogin di backend)
    @SerializedName("nama_lengkap")
    private String namaKasir;

    public String getKodeTransaksi() { return kodeTransaksi; }
    public double getTotal() { return total; }
    public double getBayar() { return bayar; }
    public double getKembalian() { return kembalian; }
    public String getShift() { return shift; }
    public String getCreatedAt() { return createdAt; }
    public String getNamaKasir() { return namaKasir; }
}
