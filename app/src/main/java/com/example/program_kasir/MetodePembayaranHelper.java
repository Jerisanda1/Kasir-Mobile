package com.example.program_kasir;

import android.graphics.Color;
import android.widget.TextView;

// Biar tampilan badge "Tunai"/"QRIS" konsisten di semua tempat (konfirmasi transaksi,
// daftar riwayat, detail riwayat), logicnya dikumpulkan di satu tempat ini saja.
public class MetodePembayaranHelper {
    public static void terapkanBadge(TextView tv, String metode) {
        if ("qris".equalsIgnoreCase(metode)) {
            tv.setText("QRIS");
            tv.setBackgroundResource(R.drawable.bg_box_purple);
            tv.setTextColor(Color.parseColor("#5149E5"));
        } else {
            tv.setText("Tunai");
            tv.setBackgroundResource(R.drawable.bg_box_green);
            tv.setTextColor(Color.parseColor("#0F6E56"));
        }
    }

    public static String labelSaja(String metode) {
        return "qris".equalsIgnoreCase(metode) ? "QRIS" : "Tunai";
    }
}
