package com.example.program_kasir;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.program_kasir.api.ApiService;
import com.example.program_kasir.model.BukaShiftRequest;
import com.example.program_kasir.model.ShiftKasResponse;
import com.example.program_kasir.model.ShiftKasSesi;
import com.example.program_kasir.model.TutupShiftRequest;
import com.example.program_kasir.model.TutupShiftResponse;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShiftKasHelper {

    private static final NumberFormat FORMAT_RUPIAH = NumberFormat.getInstance(new Locale("id", "ID"));

    public static void pastikanShiftSiap(Activity activity, ApiService apiService, SessionManager sessionManager,
                                         Runnable jikaSiap) {
        apiService.getShiftAktif(sessionManager.getBearerToken()).enqueue(new Callback<ShiftKasResponse>() {
            @Override
            public void onResponse(Call<ShiftKasResponse> call, Response<ShiftKasResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ShiftKasSesi sesiAktif = response.body().getData();
                    if (sesiAktif != null) {
                        jikaSiap.run();
                    } else {
                        tampilkanDialogBukaShift(activity, apiService, sessionManager, jikaSiap);
                    }
                } else {
                    tampilkanDialogGagalCekShift(activity, apiService, sessionManager, jikaSiap);
                }
            }

            @Override
            public void onFailure(Call<ShiftKasResponse> call, Throwable t) {
                tampilkanDialogGagalCekShift(activity, apiService, sessionManager, jikaSiap);
            }
        });
    }

    private static void tampilkanDialogGagalCekShift(Activity activity, ApiService apiService,
                                                     SessionManager sessionManager, Runnable jikaSiap) {
        new AlertDialog.Builder(activity)
                .setTitle("Tidak Bisa Terhubung")
                .setMessage("Gagal mengecek status shift. Pastikan koneksi internet aktif, lalu coba lagi.")
                .setCancelable(false)
                .setPositiveButton("Coba Lagi", (d, w) -> pastikanShiftSiap(activity, apiService, sessionManager, jikaSiap))
                .setNegativeButton("Keluar", (d, w) -> {
                    sessionManager.clearSession();
                    activity.startActivity(new android.content.Intent(activity, LoginActivity.class));
                    activity.finish();
                })
                .show();
    }

    private static void tampilkanDialogBukaShift(Activity activity, ApiService apiService,
                                                 SessionManager sessionManager, Runnable jikaSiap) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_buka_shift, null);
        EditText etModalAwal = dialogView.findViewById(R.id.etModalAwal);
        Button btnBuka = dialogView.findViewById(R.id.btnBukaShift);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnBuka.setOnClickListener(v -> {
            String input = etModalAwal.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                Toast.makeText(activity, "Modal awal wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            double modalAwal;
            try {
                modalAwal = Double.parseDouble(input);
            } catch (NumberFormatException e) {
                Toast.makeText(activity, "Modal awal tidak valid", Toast.LENGTH_SHORT).show();
                return;
            }

            btnBuka.setEnabled(false);
            apiService.bukaShift(sessionManager.getBearerToken(), new BukaShiftRequest(modalAwal))
                    .enqueue(new Callback<ShiftKasResponse>() {
                        @Override
                        public void onResponse(Call<ShiftKasResponse> call, Response<ShiftKasResponse> response) {
                            btnBuka.setEnabled(true);
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                Toast.makeText(activity, "✓ Shift dibuka, selamat bekerja!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                jikaSiap.run();
                            } else {
                                Toast.makeText(activity, "Gagal membuka shift, coba lagi", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ShiftKasResponse> call, Throwable t) {
                            btnBuka.setEnabled(true);
                            Toast.makeText(activity, "Tidak bisa terhubung: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }

    public static void mulaiTutupShift(Activity activity, ApiService apiService, SessionManager sessionManager,
                                       Runnable setelahSelesai) {
        apiService.getShiftAktif(sessionManager.getBearerToken()).enqueue(new Callback<ShiftKasResponse>() {
            @Override
            public void onResponse(Call<ShiftKasResponse> call, Response<ShiftKasResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getData() != null) {
                    tampilkanDialogTutupShift(activity, apiService, sessionManager, response.body().getData(), setelahSelesai);
                } else {
                    Toast.makeText(activity, "Tidak ada shift aktif untuk ditutup", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ShiftKasResponse> call, Throwable t) {
                Toast.makeText(activity, "Tidak bisa terhubung: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private static void tampilkanDialogTutupShift(Activity activity, ApiService apiService, SessionManager sessionManager,
                                                  ShiftKasSesi sesiAktif, Runnable setelahSelesai) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_tutup_shift, null);

        TextView tvModalAwal      = dialogView.findViewById(R.id.tvModalAwalTutup);
        TextView tvPenjualan      = dialogView.findViewById(R.id.tvPenjualanTutup);
        TextView tvKasSeharusnya  = dialogView.findViewById(R.id.tvKasSeharusnyaTutup);
        EditText etKasFisik       = dialogView.findViewById(R.id.etKasFisik);
        Button btnBatal           = dialogView.findViewById(R.id.btnBatalTutupShift);
        Button btnKonfirmasi      = dialogView.findViewById(R.id.btnKonfirmasiTutupShift);

        tvModalAwal.setText("Rp " + FORMAT_RUPIAH.format(sesiAktif.getModalAwal()));
        tvPenjualan.setText("Rp " + FORMAT_RUPIAH.format(sesiAktif.getTotalPenjualanTunai()));
        tvKasSeharusnya.setText("Rp " + FORMAT_RUPIAH.format(sesiAktif.hitungKasSeharusnya()));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnBatal.setOnClickListener(v -> dialog.dismiss());

        btnKonfirmasi.setOnClickListener(v -> {
            String input = etKasFisik.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                Toast.makeText(activity, "Kas fisik wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            double kasFisik;
            try {
                kasFisik = Double.parseDouble(input);
            } catch (NumberFormatException e) {
                Toast.makeText(activity, "Nominal tidak valid", Toast.LENGTH_SHORT).show();
                return;
            }

            btnKonfirmasi.setEnabled(false);
            apiService.tutupShift(sessionManager.getBearerToken(), new TutupShiftRequest(kasFisik))
                    .enqueue(new Callback<TutupShiftResponse>() {
                        @Override
                        public void onResponse(Call<TutupShiftResponse> call, Response<TutupShiftResponse> response) {
                            btnKonfirmasi.setEnabled(true);
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                dialog.dismiss();
                                tampilkanHasilTutupShift(activity, response.body().getData(), setelahSelesai);
                            } else {
                                Toast.makeText(activity, "Gagal menutup shift, coba lagi", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<TutupShiftResponse> call, Throwable t) {
                            btnKonfirmasi.setEnabled(true);
                            Toast.makeText(activity, "Tidak bisa terhubung: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }

    private static void tampilkanHasilTutupShift(Activity activity, TutupShiftResponse.RingkasanTutupShift hasil,
                                                 Runnable setelahSelesai) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_hasil_tutup_shift, null);

        TextView tvModalAwal   = dialogView.findViewById(R.id.tvHasilModalAwal);
        TextView tvPenjualan   = dialogView.findViewById(R.id.tvHasilPenjualan);
        TextView tvKasSeharusnya = dialogView.findViewById(R.id.tvHasilKasSeharusnya);
        TextView tvKasFisik    = dialogView.findViewById(R.id.tvHasilKasFisik);
        TextView tvSelisih     = dialogView.findViewById(R.id.tvHasilSelisih);
        TextView tvKeterangan  = dialogView.findViewById(R.id.tvKeteranganSelisih);
        Button btnOk           = dialogView.findViewById(R.id.btnTutupHasilShift);

        tvModalAwal.setText("Rp " + FORMAT_RUPIAH.format(hasil.getModalAwal()));
        tvPenjualan.setText("Rp " + FORMAT_RUPIAH.format(hasil.getTotalPenjualanTunai()));
        tvKasSeharusnya.setText("Rp " + FORMAT_RUPIAH.format(hasil.getKasSeharusnya()));
        tvKasFisik.setText("Rp " + FORMAT_RUPIAH.format(hasil.getKasFisikAkhir()));

        double selisih = hasil.getSelisih();
        String tandaSelisih = selisih > 0 ? "+" : (selisih < 0 ? "-" : "");
        tvSelisih.setText(tandaSelisih + "Rp " + FORMAT_RUPIAH.format(Math.abs(selisih)));

        if (selisih == 0) {
            tvSelisih.setTextColor(0xFF2E7D32);
            tvKeterangan.setText("Kas pas, tidak ada selisih. Kerja bagus!");
        } else if (selisih < 0) {
            tvSelisih.setTextColor(0xFFC62828);
            tvKeterangan.setText("Kas fisik lebih SEDIKIT dari seharusnya (kurang).");
        } else {
            tvSelisih.setTextColor(0xFFF57F17);
            tvKeterangan.setText("Kas fisik lebih BANYAK dari seharusnya (lebih).");
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            setelahSelesai.run();
        });

        dialog.show();
    }
}