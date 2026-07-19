package com.example.program_kasir;

import java.util.List;

// Wadah semua data yang perlu ditampilkan di struk, dikumpulkan dulu di sini
// (baik dari transaksi yang baru saja selesai, maupun dari Riwayat Transaksi)
// sebelum dikirim ke PrinterHelper untuk dicetak.
public class StrukData {
    public String kodeTransaksi;
    public String namaKasir;
    public String waktu;   // sudah dalam format teks siap cetak, misal "18/07/2026 14:30"
    public String shift;   // "1", "2", atau "ADMIN"
    public List<StrukItem> items;
    public double total;
    public double bayar;
    public double kembalian;
}
