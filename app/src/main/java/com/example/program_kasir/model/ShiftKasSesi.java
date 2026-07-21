package com.example.program_kasir.model;

import com.google.gson.annotations.SerializedName;

public class ShiftKasSesi {

    @SerializedName("kode_shift_sesi")
    private String kodeShiftSesi;

    @SerializedName("kode_kasir")
    private String kodeKasir;

    private String shift;
    private String tanggal;

    @SerializedName("modal_awal")
    private double modalAwal;

    @SerializedName("jam_buka")
    private String jamBuka;

    @SerializedName("jam_tutup")
    private String jamTutup;

    // Diisi live oleh server (dihitung real-time) baik saat sesi masih aktif maupun sudah ditutup
    @SerializedName("total_penjualan_tunai")
    private double totalPenjualanTunai;

    public String getKodeShiftSesi() { return kodeShiftSesi; }
    public String getKodeKasir() { return kodeKasir; }
    public String getShift() { return shift; }
    public String getTanggal() { return tanggal; }
    public double getModalAwal() { return modalAwal; }
    public String getJamBuka() { return jamBuka; }
    public String getJamTutup() { return jamTutup; }
    public double getTotalPenjualanTunai() { return totalPenjualanTunai; }

    // Kas seharusnya = modal awal + total penjualan tunai selama sesi ini berjalan
    public double hitungKasSeharusnya() {
        return modalAwal + totalPenjualanTunai;
    }
}