package com.example.program_kasir.model;

import com.google.gson.annotations.SerializedName;

public class TutupShiftResponse {
    private boolean success;
    private String message;
    private RingkasanTutupShift data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public RingkasanTutupShift getData() { return data; }

    public static class RingkasanTutupShift {
        @SerializedName("kode_shift_sesi")
        private String kodeShiftSesi;

        @SerializedName("modal_awal")
        private double modalAwal;

        @SerializedName("total_penjualan_tunai")
        private double totalPenjualanTunai;

        @SerializedName("kas_seharusnya")
        private double kasSeharusnya;

        @SerializedName("kas_fisik_akhir")
        private double kasFisikAkhir;

        private double selisih;

        @SerializedName("jam_buka")
        private String jamBuka;

        @SerializedName("jam_tutup")
        private String jamTutup;

        public String getKodeShiftSesi() { return kodeShiftSesi; }
        public double getModalAwal() { return modalAwal; }
        public double getTotalPenjualanTunai() { return totalPenjualanTunai; }
        public double getKasSeharusnya() { return kasSeharusnya; }
        public double getKasFisikAkhir() { return kasFisikAkhir; }
        public double getSelisih() { return selisih; }
        public String getJamBuka() { return jamBuka; }
        public String getJamTutup() { return jamTutup; }
    }
}