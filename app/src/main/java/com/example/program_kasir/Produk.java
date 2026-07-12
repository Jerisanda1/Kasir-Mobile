package com.example.program_kasir;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Produk {
    @SerializedName("kode_produk")
    private int kodeProduk;

    @SerializedName("nama_produk")
    private String nama;

    @SerializedName("harga")
    private double harga;

    @SerializedName("stok")
    private int stok;

    @SerializedName("barcode")
    private String barcode;

    @SerializedName("foto_url")
    private String fotoUrl;

    @SerializedName("tanggal_exp")
    private String tanggalExp;
    @SerializedName("nama_jenis")
    private String namaJenis;

    public String getNamaJenis() { return namaJenis; }
    public int getKodeProduk() { return kodeProduk; }
    public String getNama() { return nama; }
    public double getHarga() { return harga; }
    public int getStok() { return stok; }
    public String getBarcode() { return barcode; }
    public String getFotoUrl() { return fotoUrl; }
    public String getTanggalExp() { return tanggalExp; }

    public String getIconEmoji() {
        if (nama == null || nama.isEmpty()) return "📦";
        return String.valueOf(nama.charAt(0)).toUpperCase();
    }

    // Cek apakah stok habis
    public boolean isStokHabis() {
        return stok <= 0;
    }

    // Cek apakah produk sudah lewat tanggal kadaluarsa
    public boolean isExpired() {
        if (tanggalExp == null || tanggalExp.isEmpty()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date tglExp = sdf.parse(tanggalExp);
            Date sekarang = new Date();
            return tglExp != null && tglExp.before(sekarang);
        } catch (Exception e) {
            return false; // kalau format tanggal aneh, anggap tidak expired (aman)
        }
    }

    // Produk tidak bisa dijual kalau salah satu dari ini true
    public boolean isTidakBisaDijual() {
        return isStokHabis() || isExpired();
    }

    // Label yang ditampilkan di card kalau tidak bisa dijual
    public String getLabelTidakBisaDijual() {
        if (isExpired()) return "KADALUARSA";
        if (isStokHabis()) return "STOK HABIS";
        return "";
    }
}