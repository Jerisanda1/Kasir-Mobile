package com.example.program_kasir;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.text.NumberFormat;
import java.util.Locale;

// Menampilkan preview nota SEBELUM benar-benar dikirim ke printer Bluetooth.
// Isinya sengaja dibuat mirip struk kertas asli (font monospace, garis pemisah)
// dan urutannya sama persis dengan PrinterHelper.bangunTeksStruk(), supaya apa
// yang dilihat kasir di preview = apa yang benar-benar tercetak di kertas.
public class NotaPreviewHelper {

    public interface OnKonfirmasiCetak {
        void cetakSekarang();
    }

    public static void tampilkan(Context ctx, StrukData data, OnKonfirmasiCetak aksiCetak) {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("id", "ID"));

        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_preview_nota, null);

        TextView tvNo      = dialogView.findViewById(R.id.tvPreviewNo);
        TextView tvKasir   = dialogView.findViewById(R.id.tvPreviewKasir);
        TextView tvTanggal = dialogView.findViewById(R.id.tvPreviewTanggal);
        TextView tvShift   = dialogView.findViewById(R.id.tvPreviewShift);
        LinearLayout llItem = dialogView.findViewById(R.id.llPreviewItem);
        TextView tvTotal   = dialogView.findViewById(R.id.tvPreviewTotal);
        TextView tvBayar   = dialogView.findViewById(R.id.tvPreviewBayar);
        TextView tvKembali = dialogView.findViewById(R.id.tvPreviewKembali);
        Button btnCetak    = dialogView.findViewById(R.id.btnPreviewCetak);
        Button btnTutup    = dialogView.findViewById(R.id.btnPreviewTutup);

        tvNo.setText("No: " + data.kodeTransaksi);
        tvKasir.setText("Kasir: " + data.namaKasir);
        tvTanggal.setText("Tanggal: " + data.waktu);
        tvShift.setText("Shift: " + ("ADMIN".equalsIgnoreCase(data.shift) ? "Admin" : data.shift));

        llItem.removeAllViews();
        if (data.items != null) {
            for (StrukItem item : data.items) {
                View baris = LayoutInflater.from(ctx).inflate(R.layout.item_preview_nota, llItem, false);
                TextView tvNamaProduk = baris.findViewById(R.id.tvPreviewNamaProduk);
                TextView tvQtyHarga   = baris.findViewById(R.id.tvPreviewQtyHarga);
                TextView tvSubtotal   = baris.findViewById(R.id.tvPreviewSubtotal);

                tvNamaProduk.setText(item.namaProduk);
                tvQtyHarga.setText(item.qty + " x Rp " + fmt.format(item.harga));
                tvSubtotal.setText("Rp " + fmt.format(item.subtotal));
                llItem.addView(baris);
            }
        }

        tvTotal.setText("Rp " + fmt.format(data.total));
        tvBayar.setText("Rp " + fmt.format(data.bayar));
        tvKembali.setText("Rp " + fmt.format(data.kembalian));

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnCetak.setOnClickListener(v -> {
            dialog.dismiss();
            if (aksiCetak != null) aksiCetak.cetakSekarang();
        });
        btnTutup.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Batasi lebar dialog secara eksplisit SETELAH show() (resize window cuma efektif
        // kalau window-nya sudah tampil). Default AlertDialog di tablet cenderung melebar
        // penuh mengikuti layar, padahal isinya cuma kertas nota yang sempit.
        if (dialog.getWindow() != null) {
            float density = ctx.getResources().getDisplayMetrics().density;
            int lebarPx = Math.round(380 * density);
            dialog.getWindow().setLayout(lebarPx, android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }
}
