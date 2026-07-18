package com.example.program_kasir;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Halaman Riwayat Transaksi. Sidebar & logout sekarang tinggal di MainActivity;
// fragment ini cuma mengurus isi kolom kanan (search, filter tanggal, daftar riwayat).
public class RiwayatFragment extends Fragment {

    private final List<TransaksiRiwayat> daftarRiwayat = new ArrayList<>();
    // Menyimpan HASIL LENGKAP dari server untuk filter/query yang sedang aktif (belum dipotong per halaman)
    private final List<TransaksiRiwayat> daftarRiwayatSemua = new ArrayList<>();

    private static final int JUMLAH_PER_HALAMAN = 10;
    private int halamanSekarang = 1; // 1-indexed

    private RecyclerView rvRiwayat;
    private RiwayatAdapter riwayatAdapter;

    private EditText etSearch, etTanggal;
    private Button btnReset, btnPrevHalaman, btnNextHalaman;
    private LinearLayout llKosong, llNomorHalamanContainer;
    private ProgressBar progressBar;
    private TextView tvInfoHalaman, tvSubjudul;

    private String tanggalDipilih = null; // format yyyy-MM-dd, null kalau tidak difilter

    private SessionManager sessionManager;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_riwayat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inisialisasiView(view);

        Context ctx = requireContext();
        sessionManager = new SessionManager(ctx);
        apiService = ApiClient.getClient().create(ApiService.class);

        setupRecyclerView();
        setupListener();

        muatRiwayat(null, null);
    }

    private void inisialisasiView(View v) {
        rvRiwayat               = v.findViewById(R.id.rvRiwayat);
        etSearch                = v.findViewById(R.id.etSearch);
        etTanggal               = v.findViewById(R.id.etTanggal);
        btnReset                = v.findViewById(R.id.btnReset);
        llKosong                = v.findViewById(R.id.llKosong);
        llNomorHalamanContainer = v.findViewById(R.id.llNomorHalamanContainer);
        progressBar             = v.findViewById(R.id.progressBar);
        tvSubjudul              = v.findViewById(R.id.tvSubjudul);
        tvInfoHalaman           = v.findViewById(R.id.tvInfoHalaman);
        btnPrevHalaman          = v.findViewById(R.id.btnPrevHalaman);
        btnNextHalaman          = v.findViewById(R.id.btnNextHalaman);
    }

    private void setupRecyclerView() {
        rvRiwayat.setLayoutManager(new LinearLayoutManager(requireContext()));
        riwayatAdapter = new RiwayatAdapter(daftarRiwayat, new RiwayatAdapter.OnRiwayatActionListener() {
            @Override
            public void onDetailClick(TransaksiRiwayat item) {
                tampilkanDetail(item);
            }

            @Override
            public void onCetakClick(TransaksiRiwayat item) {
                cetakNota(item);
            }
        });
        rvRiwayat.setAdapter(riwayatAdapter);
    }

    private void setupListener() {
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

        // Navigasi halaman: cukup potong ulang dari data yang sudah ada di memori,
        // tidak perlu request API lagi
        btnPrevHalaman.setOnClickListener(v -> {
            if (halamanSekarang > 1) {
                halamanSekarang--;
                tampilkanHalaman();
            }
        });

        btnNextHalaman.setOnClickListener(v -> {
            int totalHalaman = hitungTotalHalaman();
            if (halamanSekarang < totalHalaman) {
                halamanSekarang++;
                tampilkanHalaman();
            }
        });
    }

    private void tampilkanDatePicker() {
        Context ctx = requireContext();
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(ctx, (view, year, month, dayOfMonth) -> {
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

        boolean tanpaFilter = (search == null || search.isEmpty()) && (tanggal == null || tanggal.isEmpty());

        apiService.getRiwayat(sessionManager.getBearerToken(),
                        (search == null || search.isEmpty()) ? null : search,
                        tanggal)
                .enqueue(new Callback<RiwayatResponse>() {
                    @Override
                    public void onResponse(Call<RiwayatResponse> call, Response<RiwayatResponse> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            List<TransaksiRiwayat> hasilServer = response.body().getData();

                            daftarRiwayatSemua.clear();
                            if (tanpaFilter) {
                                // Tidak ada endpoint untuk filter per-bulan di server,
                                // jadi default "bulan ini" disaring di HP dari seluruh riwayat.
                                daftarRiwayatSemua.addAll(filterBulanIni(hasilServer));
                            } else {
                                daftarRiwayatSemua.addAll(hasilServer);
                            }

                            perbaruiSubjudul(tanpaFilter);
                            halamanSekarang = 1;
                            tampilkanHalaman();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Gagal mengambil riwayat transaksi", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RiwayatResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                "Tidak bisa terhubung ke server: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Saring list dari server, hanya sisakan transaksi yang createdAt-nya di bulan berjalan.
    // Format createdAt dari backend: "yyyy-MM-dd HH:mm:ss", jadi cukup bandingkan 7 karakter awal.
    private List<TransaksiRiwayat> filterBulanIni(List<TransaksiRiwayat> semua) {
        List<TransaksiRiwayat> hasil = new ArrayList<>();
        if (semua == null) return hasil;

        String bulanIni = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        for (TransaksiRiwayat t : semua) {
            String createdAt = t.getCreatedAt();
            if (createdAt != null && createdAt.length() >= 7 && createdAt.substring(0, 7).equals(bulanIni)) {
                hasil.add(t);
            }
        }
        return hasil;
    }

    // Update teks kecil di header: kasih tahu user lagi lihat "bulan ini" atau hasil filter
    private void perbaruiSubjudul(boolean tanpaFilter) {
        if (tanpaFilter) {
            String namaBulan = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID")).format(new Date());
            tvSubjudul.setText("Menampilkan transaksi bulan " + namaBulan);
        } else {
            tvSubjudul.setText("Menampilkan hasil pencarian/filter yang kamu pilih.");
        }
    }

    private int hitungTotalHalaman() {
        return Math.max(1, (int) Math.ceil(daftarRiwayatSemua.size() / (double) JUMLAH_PER_HALAMAN));
    }

    // Potong daftarRiwayatSemua sesuai halaman yang sedang aktif lalu tampilkan ke RecyclerView.
    // Tidak perlu request API lagi karena data lengkap sudah ada di memori.
    private void tampilkanHalaman() {
        llKosong.setVisibility(daftarRiwayatSemua.isEmpty() ? View.VISIBLE : View.GONE);

        int totalItem = daftarRiwayatSemua.size();
        int totalHalaman = hitungTotalHalaman();
        if (halamanSekarang > totalHalaman) halamanSekarang = totalHalaman;
        if (halamanSekarang < 1) halamanSekarang = 1;

        int mulai = (halamanSekarang - 1) * JUMLAH_PER_HALAMAN;
        int akhir = Math.min(mulai + JUMLAH_PER_HALAMAN, totalItem);

        daftarRiwayat.clear();
        if (mulai < akhir) {
            daftarRiwayat.addAll(daftarRiwayatSemua.subList(mulai, akhir));
        }
        riwayatAdapter.notifyDataSetChanged();

        tvInfoHalaman.setText(totalItem == 0
                ? "Tidak ada transaksi"
                : "Halaman " + halamanSekarang + " dari " + totalHalaman + " (" + totalItem + " transaksi)");

        btnPrevHalaman.setEnabled(halamanSekarang > 1);
        btnNextHalaman.setEnabled(halamanSekarang < totalHalaman);

        // GENERATE NOMOR HALAMAN DINAMIS <prev 1 2 3 next>
        llNomorHalamanContainer.removeAllViews(); // Bersihkan tombol nomor lama

        if (totalItem > 0) {
            for (int i = 1; i <= totalHalaman; i++) {
                final int nomorHalaman = i;

                // Buat tombol baru via programmatically
                Button btnPage = new Button(requireContext());

                // Mengatur ukuran tombol (kotak kecil)
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dpToPx(40) // Tinggi disamakan 40dp dengan tombol prev/next
                );
                params.setMargins(dpToPx(2), 0, dpToPx(2), 0); // Jarak antar angka 2dp
                btnPage.setLayoutParams(params);
                btnPage.setMinWidth(dpToPx(40)); // Menjaga agar tombol berbentuk kotak/proporsional
                btnPage.setPadding(0, 0, 0, 0);

                btnPage.setText(String.valueOf(nomorHalaman));
                btnPage.setTextSize(12);
                btnPage.setTypeface(null, android.graphics.Typeface.BOLD);
                btnPage.setStateListAnimator(null); // Hilangkan shadow bawaan material button

                // Buat shape rounded rectangle untuk tiap angka (Radius 8dp)
                GradientDrawable bgShape = new GradientDrawable();
                bgShape.setShape(GradientDrawable.RECTANGLE);
                bgShape.setCornerRadius(dpToPx(8));

                // Kondisi jika halaman ini SEDANG DIKUNJUNGI (Aktif)
                if (nomorHalaman == halamanSekarang) {
                    bgShape.setColor(android.graphics.Color.parseColor("#5149E5"));
                    btnPage.setTextColor(android.graphics.Color.WHITE);
                } else {
                    // Halaman tidak aktif
                    bgShape.setColor(android.graphics.Color.WHITE);
                    btnPage.setTextColor(android.graphics.Color.parseColor("#1A1A2E"));
                }

                // Terapkan background yang telah dibuat
                btnPage.setBackground(bgShape);

                // Klik nomor halaman
                btnPage.setOnClickListener(v -> {
                    halamanSekarang = nomorHalaman;
                    tampilkanHalaman();
                });

                // Masukkan ke dalam container
                llNomorHalamanContainer.addView(btnPage);
            }
        }
    }

    // TODO: cetak nota beneran menyusul setelah mekanisme print (printer/PDF) disepakati.
    private void cetakNota(TransaksiRiwayat item) {
        Toast.makeText(requireContext(),
                "Cetak nota " + item.getKodeTransaksi() + " (menyusul)", Toast.LENGTH_SHORT).show();
    }

    // Ambil detail 1 transaksi dari API lalu tampilkan dalam dialog
    private void tampilkanDetail(TransaksiRiwayat item) {
        Toast.makeText(requireContext(), "Memuat detail...", Toast.LENGTH_SHORT).show();

        apiService.getDetailRiwayat(sessionManager.getBearerToken(), item.getKodeTransaksi())
                .enqueue(new Callback<DetailRiwayatResponse>() {
                    @Override
                    public void onResponse(Call<DetailRiwayatResponse> call, Response<DetailRiwayatResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            bukaDialogDetail(response.body());
                        } else {
                            Toast.makeText(requireContext(),
                                    "Gagal mengambil detail transaksi", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<DetailRiwayatResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                "Tidak bisa terhubung ke server: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void bukaDialogDetail(DetailRiwayatResponse detail) {
        Context ctx = requireContext();
        TransaksiRiwayat transaksi = detail.getData();
        List<ItemDetailRiwayat> items = detail.getItems();
        NumberFormat fmt = NumberFormat.getInstance(new Locale("id", "ID"));

        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_detail_riwayat, null);

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
                View itemView = LayoutInflater.from(ctx)
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

        AlertDialog dialog = new AlertDialog.Builder(ctx)
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

    // Helper untuk mengubah ukuran DP menjadi Pixel di runtime Java
    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
