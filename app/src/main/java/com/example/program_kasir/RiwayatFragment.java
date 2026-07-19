package com.example.program_kasir;

import android.Manifest;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    // Menyimpan struk yang lagi nunggu izin Bluetooth diberikan, biar bisa lanjut cetak setelah izin di-ACC
    private StrukData strukMenunggu;

    private final PrinterHelper.PrintCallback printCallback = new PrinterHelper.PrintCallback() {
        @Override
        public void onSukses() {
            if (isAdded()) Toast.makeText(requireContext(), "Struk berhasil dicetak", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onGagal(String pesanError) {
            if (isAdded()) Toast.makeText(requireContext(), "Gagal cetak: " + pesanError, Toast.LENGTH_LONG).show();
        }
    };

    private final ActivityResultLauncher<String> izinBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), diizinkan -> {
                if (diizinkan && strukMenunggu != null) {
                    PrinterHelper.pilihPrinterDanCetak(requireContext(), strukMenunggu, printCallback);
                } else if (!diizinkan) {
                    Toast.makeText(requireContext(), "Izin Bluetooth diperlukan untuk cetak struk", Toast.LENGTH_SHORT).show();
                }
                strukMenunggu = null;
            });

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

        // Hint tanggal contoh selalu tanggal hari ini, bukan teks statis
        SimpleDateFormat formatTampil = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etTanggal.setHint(formatTampil.format(new Date()));
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

        // Kalau user belum cari/filter apapun, minta server cuma kirim transaksi BULAN INI saja
        // (jadi bukan ambil semua riwayat dari awal terus disaring di HP)
        String bulanSekarang = tanpaFilter
                ? new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date())
                : null;

        apiService.getRiwayat(sessionManager.getBearerToken(),
                        (search == null || search.isEmpty()) ? null : search,
                        tanggal,
                        bulanSekarang)
                .enqueue(new Callback<RiwayatResponse>() {
                    @Override
                    public void onResponse(Call<RiwayatResponse> call, Response<RiwayatResponse> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            daftarRiwayatSemua.clear();
                            daftarRiwayatSemua.addAll(response.body().getData());

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

                // Mengatur ukuran tombol (bulat kecil, minimalis)
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        dpToPx(28),
                        dpToPx(28)
                );
                params.setMargins(dpToPx(2), 0, dpToPx(2), 0); // Jarak antar angka 2dp
                btnPage.setLayoutParams(params);
                btnPage.setMinWidth(0);
                btnPage.setMinHeight(0);
                btnPage.setPadding(0, 0, 0, 0);

                btnPage.setText(String.valueOf(nomorHalaman));
                btnPage.setTextSize(11);
                btnPage.setStateListAnimator(null); // Hilangkan shadow bawaan material button

                // Kondisi jika halaman ini SEDANG DIKUNJUNGI (Aktif)
                if (nomorHalaman == halamanSekarang) {
                    // Halaman aktif: bulat penuh warna ungu, teks putih tebal
                    GradientDrawable bgAktif = new GradientDrawable();
                    bgAktif.setShape(GradientDrawable.OVAL);
                    bgAktif.setColor(android.graphics.Color.parseColor("#5149E5"));
                    btnPage.setBackground(bgAktif);
                    btnPage.setTextColor(android.graphics.Color.WHITE);
                    btnPage.setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    // Halaman tidak aktif: tanpa background, cuma teks abu-abu
                    btnPage.setBackground(null);
                    btnPage.setTextColor(android.graphics.Color.parseColor("#78909C"));
                    btnPage.setTypeface(null, android.graphics.Typeface.NORMAL);
                }

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

    // Ambil detail transaksi dari API dulu (isi item belum ada di data list), baru dicetak
    private void cetakNota(TransaksiRiwayat item) {
        Toast.makeText(requireContext(), "Menyiapkan struk...", Toast.LENGTH_SHORT).show();

        apiService.getDetailRiwayat(sessionManager.getBearerToken(), item.getKodeTransaksi())
                .enqueue(new Callback<DetailRiwayatResponse>() {
                    @Override
                    public void onResponse(Call<DetailRiwayatResponse> call, Response<DetailRiwayatResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            StrukData data = bangunStrukData(response.body());
                            NotaPreviewHelper.tampilkan(requireContext(), data, () -> mintaIzinLaluCetak(data));
                        } else {
                            Toast.makeText(requireContext(),
                                    "Gagal mengambil detail transaksi untuk dicetak", Toast.LENGTH_SHORT).show();
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

    // Susun StrukData dari hasil API detail riwayat
    private StrukData bangunStrukData(DetailRiwayatResponse detail) {
        TransaksiRiwayat transaksi = detail.getData();

        StrukData data = new StrukData();
        data.kodeTransaksi = transaksi.getKodeTransaksi();
        data.namaKasir = sessionManager.getNamaLengkap();
        data.waktu = formatWaktuCetak(transaksi.getCreatedAt());
        data.shift = transaksi.getShift();
        data.total = transaksi.getTotal();
        data.bayar = transaksi.getBayar();
        data.kembalian = transaksi.getKembalian();

        data.items = new ArrayList<>();
        if (detail.getItems() != null) {
            for (ItemDetailRiwayat itemProduk : detail.getItems()) {
                data.items.add(new StrukItem(
                        itemProduk.getNamaProduk(),
                        itemProduk.getQty(),
                        itemProduk.getHarga(),
                        itemProduk.getSubtotal()
                ));
            }
        }
        return data;
    }

    private String formatWaktuCetak(String createdAt) {
        if (createdAt == null) return "-";
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return output.format(input.parse(createdAt));
        } catch (Exception e) {
            return createdAt;
        }
    }

    // Cek izin Bluetooth dulu sebelum benar-benar cetak; kalau belum ada, minta izin dulu
    private void mintaIzinLaluCetak(StrukData data) {
        if (PrinterHelper.izinBluetoothSudahAda(requireContext())) {
            PrinterHelper.pilihPrinterDanCetak(requireContext(), data, printCallback);
        } else {
            strukMenunggu = data;
            izinBluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }
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
