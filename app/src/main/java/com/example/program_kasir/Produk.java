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
        if (isExpired()) return "EXPIRED";
        if (isStokHabis()) return "STOK HABIS";
        return "";
    }
    private static final int BATAS_STOK_MENIPIS = 5;   // sesuaikan kalau perlu
    private static final int BATAS_HARI_AKAN_EXPIRED = 7;

    // Stok menipis: masih bisa dijual, tapi tinggal sedikit
    public boolean isStokMenipis() {
        return !isStokHabis() && stok <= BATAS_STOK_MENIPIS;
    }

    // Akan kadaluarsa dalam waktu dekat (belum lewat, tapi mendekati)
    public boolean isAkanExpired() {
        if (tanggalExp == null || tanggalExp.isEmpty() || isExpired()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date tglExp = sdf.parse(tanggalExp);
            if (tglExp == null) return false;
            long selisihMs = tglExp.getTime() - new Date().getTime();
            long selisihHari = selisihMs / (1000 * 60 * 60 * 24);
            return selisihHari >= 0 && selisihHari <= BATAS_HARI_AKAN_EXPIRED;
        } catch (Exception e) {
            return false;
        }
    }

    // Label peringatan (beda dari label "tidak bisa dijual" yang sudah ada)
    public String getLabelPeringatan() {
        if (isAkanExpired()) return "SEGERA EXPIRED";
        if (isStokMenipis()) return "STOK MENIPIS";
        return "";
    }

    public boolean adaPeringatan() {
        return !isTidakBisaDijual() && (isStokMenipis() || isAkanExpired());
    }

    // Dipakai TransaksiFragment supaya angka stok di card berkurang/bertambah realtime
    public void kurangiStok(int qty) {
        stok -= qty;
        if (stok < 0) stok = 0;
    }

    public void tambahStok(int qty) {
        stok += qty;
    }
}