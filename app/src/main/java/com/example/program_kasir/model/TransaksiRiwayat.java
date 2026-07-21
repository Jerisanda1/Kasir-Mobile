package com.example.program_kasir.model;

import com.google.gson.annotations.SerializedName;

public class TransaksiRiwayat {

    @SerializedName(value = "kode_transaksi", alternate = {"kode", "id_transaksi", "no_nota"})
    private String kodeTransaksi;

    @SerializedName(value = "total", alternate = {"total_bayar", "net_total", "grand_total", "total_akhir"})
    private double total;

    @SerializedName(value = "diskon", alternate = {"nilai_diskon", "potongan", "discount", "total_diskon", "potongan_harga", "discount_amount", "potongan_nominal"})
    private double diskon;

    @SerializedName(value = "bayar", alternate = {"jumlah_bayar", "cash", "pembayaran", "tunai"})
    private double bayar;

    @SerializedName(value = "kembalian", alternate = {"kembali", "kembalian_nominal", "change"})
    private double kembalian;

    @SerializedName("shift")
    private String shift;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName(value = "nama_lengkap", alternate = {"nama_kasir", "kasir"})
    private String namaKasir;

    public String getKodeTransaksi() { return kodeTransaksi; }
    public double getTotal() { return total; }
    public double getDiskon() { return diskon; }
    public double getBayar() { return bayar; }
    public double getKembalian() { return kembalian; }
    public String getShift() { return shift; }
    public String getCreatedAt() { return createdAt; }
    public String getNamaKasir() { return namaKasir; }
}
