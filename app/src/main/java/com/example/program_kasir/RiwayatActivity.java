package com.example.program_kasir;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.program_kasir.api.ApiClient;
import com.example.program_kasir.api.ApiService;
import com.example.program_kasir.model.DetailRiwayatResponse;
import com.example.program_kasir.model.ItemDetailRiwayat;
import com.example.program_kasir.model.RiwayatResponse;
import com.example.program_kasir.model.TransaksiRiwayat;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RiwayatActivity extends AppCompatActivity {

    private final List<TransaksiRiwayat> daftarRiwayat = new ArrayList<>();

    private RecyclerView rvRiwayat;
    private RiwayatAdapter riwayatAdapter;

    private EditText etSearch, etTanggal;
    private Button btnReset, ivLogout;
    private LinearLayout llMenuTransaksi, llKosong;
    private ProgressBar progressBar;

    private String tanggalDipilih = null; // format yyyy-MM-dd, null kalau tidak difilter

    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat);

        inisialisasiView();

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        setupRecyclerView();
        setupListener();

        muatRiwayat(null, null);
    }

    private void inisialisasiView() {
        rvRiwayat       = findViewById(R.id.rvRiwayat);
        etSearch        = findViewById(R.id.etSearch);
        etTanggal       = findViewById(R.id.etTanggal);
        btnReset        = findViewById(R.id.btnReset);
        ivLogout        = findViewById(R.id.ivLogout);
        llMenuTransaksi = findViewById(R.id.llMenuTransaksi);
        llKosong        = findViewById(R.id.llKosong);
        progressBar     = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        rvRiwayat.setLayoutManager(new LinearLayoutManager(this));
        riwayatAdapter = new RiwayatAdapter(daftarRiwayat, this::tampilkanDetail);
        rvRiwayat.setAdapter(riwayatAdapter);
    }

    private void setupListener() {
        // Kembali ke halaman Transaksi yang sudah ada di back stack
        llMenuTransaksi.setOnClickListener(v -> finish());

        ivLogout.setOnClickListener(v -> konfirmasiLogout());

        // Cari lewat tombol "search" di keyboard
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                muatRiwayat(etSearch.getText().toString().trim(), tanggalDipilih);
                return true;
            }
            return false;
        });

        // Filter tanggal pakai DatePickerDialog
        etTanggal.setOnClickListener(v -> tampilkanDatePicker());

        btnReset.setOnClickListener(v -> {
            etSearch.setText("");
            etTanggal.setText("");
            tanggalDipilih = null;
            muatRiwayat(null, null);
        });
    }

    private void tampilkanDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar dipilih = Calendar.getInstance();
            dipilih.set(year, month, dayOfMonth);

            SimpleDateFormat simpanFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat tampilFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            tanggalDipilih = simpanFormat.format(dipilih.getTime());
            etTanggal.setText(tampilFormat.format(dipilih.getTime()));

            muatRiwayat(etSearch.getText().toString().trim(), tanggalDipilih);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void muatRiwayat(String search, String tanggal) {
        progressBar.setVisibility(View.VISIBLE);
        llKosong.setVisibility(View.GONE);

        apiService.getRiwayat(sessionManager.getBearerToken(),
                        (search == null || search.isEmpty()) ? null : search,
                        tanggal)
                .enqueue(new Callback<RiwayatResponse>() {
                    @Override
                    public void onResponse(Call<RiwayatResponse> call, Response<RiwayatResponse> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            daftarRiwayat.clear();
                            daftarRiwayat.addAll(response.body().getData());
                            riwayatAdapter.notifyDataSetChanged();
                            llKosong.setVisibility(daftarRiwayat.isEmpty() ? View.VISIBLE : View.GONE);
                        } else {
                            Toast.makeText(RiwayatActivity.this,
                                    "Gagal mengambil riwayat transaksi", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RiwayatResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(RiwayatActivity.this,
                                "Tidak bisa terhubung ke server: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Ambil detail 1 transaksi dari API lalu tampilkan dalam dialog
    private void tampilkanDetail(TransaksiRiwayat item) {
        Toast.makeText(this, "Memuat detail...", Toast.LENGTH_SHORT).show();

        apiService.getDetailRiwayat(sessionManager.getBearerToken(), item.getKodeTransaksi())
                .enqueue(new Callback<DetailRiwayatResponse>() {
                    @Override
                    public void onResponse(Call<DetailRiwayatResponse> call, Response<DetailRiwayatResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            bukaDialogDetail(response.body());
                        } else {
                            Toast.makeText(RiwayatActivity.this,
                                    "Gagal mengambil detail transaksi", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<DetailRiwayatResponse> call, Throwable t) {
                        Toast.makeText(RiwayatActivity.this,
                                "Tidak bisa terhubung ke server: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void bukaDialogDetail(DetailRiwayatResponse detail) {
        TransaksiRiwayat transaksi = detail.getData();
        List<ItemDetailRiwayat> items = detail.getItems();
        NumberFormat fmt = NumberFormat.getInstance(new Locale("id", "ID"));

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_detail_riwayat, null);

        TextView tvJudul       = dialogView.findViewById(R.id.tvJudulDetailTransaksi);
        TextView tvTanggal     = dialogView.findViewById(R.id.tvTanggalDetail);
        LinearLayout llItem    = dialogView.findViewById(R.id.llDaftarItemDetail);
        TextView tvTotal       = dialogView.findViewById(R.id.tvDetailTotal);
        TextView tvBayar       = dialogView.findViewById(R.id.tvDetailBayar);
        TextView tvKembali     = dialogView.findViewById(R.id.tvDetailKembali);
        TextView tvKasirShift  = dialogView.findViewById(R.id.tvKasirShiftDetail);
        Button btnTutup        = dialogView.findViewById(R.id.btnTutupDetail);

        tvJudul.setText(transaksi.getKodeTransaksi());
        tvTanggal.setText(formatTanggalLengkap(transaksi.getCreatedAt()));
        tvTotal.setText("Rp " + fmt.format(transaksi.getTotal()));
        tvBayar.setText("Rp " + fmt.format(transaksi.getBayar()));
        tvKembali.setText("Rp " + fmt.format(transaksi.getKembalian()));

        tvKasirShift.setText("Kasir: " + sessionManager.getNamaLengkap() + "  •  Shift " + transaksi.getShift());

        llItem.removeAllViews();
        if (items != null) {
            for (ItemDetailRiwayat itemProduk : items) {
                View itemView = LayoutInflater.from(this)
                        .inflate(R.layout.item_konfirmasi_produk, llItem, false);
                TextView tvNama       = itemView.findViewById(R.id.tvNamaProdukKonfirmasi);
                TextView tvQtyHarga   = itemView.findViewById(R.id.tvQtyHargaKonfirmasi);
                TextView tvSubtotal   = itemView.findViewById(R.id.tvSubtotalProdukKonfirmasi);

                tvNama.setText(itemProduk.getNamaProduk());
                tvQtyHarga.setText(itemProduk.getQty() + " x Rp " + fmt.format(itemProduk.getHarga()));
                tvSubtotal.setText("Rp " + fmt.format(itemProduk.getSubtotal()));
                llItem.addView(itemView);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnTutup.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String formatTanggalLengkap(String createdAt) {
        if (createdAt == null) return "-";
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm", new Locale("id", "ID"));
            return output.format(input.parse(createdAt));
        } catch (Exception e) {
            return createdAt;
        }
    }

    private void konfirmasiLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Apakah Anda yakin ingin keluar?")
                .setPositiveButton("Ya, Keluar", (d, w) -> {
                    sessionManager.clearSession();
                    Intent intent = new Intent(RiwayatActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
