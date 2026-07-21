package com.example.program_kasir;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.program_kasir.api.ApiClient;
import com.example.program_kasir.api.ApiService;
import com.example.program_kasir.model.CartItem;
import com.example.program_kasir.model.ProdukResponse;
import com.example.program_kasir.model.TransaksiRequest;
import com.example.program_kasir.model.TransaksiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Halaman Transaksi (kasir jual barang). Sidebar & logout sekarang tinggal di MainActivity;
// fragment ini cuma mengurus isi kolom kanan (produk, keranjang, pembayaran).
public class TransaksiFragment extends Fragment {

    // Daftar lengkap produk yang dijual toko (diisi dari API)
    private final List<Produk> daftarProdukAsli = new ArrayList<>();
    // Daftar produk yang sedang ditampilkan (hasil filter search)
    private final List<Produk> daftarProdukTampil = new ArrayList<>();
    // Daftar isi keranjang belanja
    private final List<ItemKeranjang> daftarKeranjang = new ArrayList<>();

    private RecyclerView rvProduk, rvKeranjang;
    private ProdukAdapter produkAdapter;
    private KeranjangAdapter keranjangAdapter;

    // Menyimpan struk yang lagi nunggu izin Bluetooth diberikan
    private StrukData strukMenunggu;

    private final PrinterHelper.PrintCallback printCallback = new PrinterHelper.PrintCallback() {
        @Override
        public void onSukses() {
            if (isAdded()) Toast.makeText(requireContext(), "Struk berhasil dicetak", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onGagal(String pesanError) {
            if (!isAdded()) return;

            // Jika gagal, beri opsi untuk "Lupakan Printer" (reset MAC yang tersimpan).
            new AlertDialog.Builder(requireContext())
                    .setTitle("Gagal Mencetak")
                    .setMessage(pesanError + ".\n\nJika ini bukan printer yang benar, silakan reset pilihan printer.")
                    .setPositiveButton("Tutup", null)
                    .setNeutralButton("Reset Printer", (dialog, which) -> {
                        PrinterHelper.lupakanPrinterTersimpan(requireContext());
                        Toast.makeText(requireContext(), "Pilihan printer direset. Silakan klik Cetak lagi untuk memilih ulang.", Toast.LENGTH_LONG).show();
                    })
                    .show();
        }
    };

    // Minta BLUETOOTH_CONNECT & BLUETOOTH_SCAN sekaligus (bukan cuma BLUETOOTH_CONNECT).
    // Keduanya wajib di Android 12+ -- lihat catatan di PrinterHelper.izinBluetoothSudahAda().
    private final ActivityResultLauncher<String[]> izinBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), hasilIzin -> {
                boolean semuaDiizinkan = !hasilIzin.containsValue(false);
                if (semuaDiizinkan && strukMenunggu != null) {
                    PrinterHelper.pilihPrinterDanCetak(requireContext(), strukMenunggu, printCallback);
                } else if (!semuaDiizinkan) {
                    Toast.makeText(requireContext(), "Izin Bluetooth (Connect & Scan) diperlukan untuk cetak struk", Toast.LENGTH_SHORT).show();
                }
                strukMenunggu = null;
            });

    private EditText etSearch, etDiskon, etJumlahBayar;
    private TextView tvSubtotal, tvDiskon, tvTotalBayar, tvKembalian, tvTanggal, tvKeranjangKosong;
    private Button btnReset, btnBayar;
    private LinearLayout llKategori;
    private String kategoriDipilih = "Semua";
    private double totalBayar = 0;

    private SessionManager sessionManager;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaksi, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inisialisasiView(view);

        Context ctx = requireContext();
        sessionManager = new SessionManager(ctx);
        apiService = ApiClient.getClient().create(ApiService.class);

        siapkanDataProduk();
        setupRecyclerView();
        setupListener();

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        tvTanggal.setText(sdf.format(new java.util.Date()));

        // Tampilkan kondisi awal Rp 0
        hitungOtomatis();
    }

    private void inisialisasiView(View v) {
        rvProduk          = v.findViewById(R.id.rvProduk);
        rvKeranjang       = v.findViewById(R.id.rvKeranjang);
        etSearch          = v.findViewById(R.id.etSearch);
        etDiskon          = v.findViewById(R.id.etDiskon);
        etJumlahBayar     = v.findViewById(R.id.etJumlahBayar);
        tvSubtotal        = v.findViewById(R.id.tvSubtotal);
        tvDiskon          = v.findViewById(R.id.tvDiskon);
        tvTotalBayar      = v.findViewById(R.id.tvTotalBayar);
        tvKembalian       = v.findViewById(R.id.tvKembalian);
        tvTanggal         = v.findViewById(R.id.tvTanggal);
        tvKeranjangKosong = v.findViewById(R.id.tvKeranjangKosong);
        btnReset          = v.findViewById(R.id.btnReset);
        btnBayar          = v.findViewById(R.id.btnBayar);
        llKategori        = v.findViewById(R.id.llKategori);
    }

    // BARU: susun teks "Nama (Role - Shift jam)" di header
    // DIUBAH: sebelumnya data dummy, sekarang fetch dari API
    private void siapkanDataProduk() {
        apiService.getProduk(sessionManager.getBearerToken(), null)
                .enqueue(new Callback<ProdukResponse>() {
                    @Override
                    public void onResponse(Call<ProdukResponse> call, Response<ProdukResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            daftarProdukAsli.clear();
                            daftarProdukAsli.addAll(response.body().getData());

                            daftarProdukTampil.clear();
                            daftarProdukTampil.addAll(daftarProdukAsli);
                            urutkanBerdasarkanKetersediaan(daftarProdukTampil);

                            if (produkAdapter != null) {
                                produkAdapter.notifyDataSetChanged();
                            }
                            buatKategoriChips();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Gagal mengambil data produk dari server", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ProdukResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                "Tidak bisa terhubung ke server: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupRecyclerView() {
        // Grid produk 3 kolom
        rvProduk.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        produkAdapter = new ProdukAdapter(daftarProdukTampil, this::tambahKeKeranjang);
        rvProduk.setAdapter(produkAdapter);

        // List keranjang vertikal, dibatasi tinggi via XML agar muncul scroll
        rvKeranjang.setLayoutManager(new LinearLayoutManager(requireContext()));
        keranjangAdapter = new KeranjangAdapter(daftarKeranjang, new KeranjangAdapter.OnKeranjangActionListener() {
            @Override
            public void onTambahQty(int position) {
                ItemKeranjang item = daftarKeranjang.get(position);

                // Cegah qty melebihi stok yang tersedia
                if (item.getJumlah() + 1 > item.getProduk().getStok()) {
                    Toast.makeText(requireContext(),
                            "Stok " + item.getProduk().getNama() + " tidak mencukupi (sisa "
                                    + item.getProduk().getStok() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }

                item.setJumlah(item.getJumlah() + 1);
                keranjangAdapter.notifyItemChanged(position);
                hitungOtomatis();
            }
            @Override
            public void onKurangQty(int position) {
                ItemKeranjang item = daftarKeranjang.get(position);
                if (item.getJumlah() > 1) {
                    item.setJumlah(item.getJumlah() - 1);
                    keranjangAdapter.notifyItemChanged(position);
                } else {
                    daftarKeranjang.remove(position);
                    keranjangAdapter.notifyItemRemoved(position);
                    cekKeranjangKosong();
                }
                hitungOtomatis();
            }

            // BARU: hapus item langsung dari keranjang, berapa pun jumlahnya
            @Override
            public void onHapusItem(int position) {
                daftarKeranjang.remove(position);
                keranjangAdapter.notifyItemRemoved(position);
                cekKeranjangKosong();
                hitungOtomatis();
            }
        });
        rvKeranjang.setAdapter(keranjangAdapter);

        cekKeranjangKosong();
    }

    private void setupListener() {
        // Search produk secara realtime (tetap jalan baik buat ketik manual maupun sisa karakter dari scan)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProduk(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Kotak yang sama juga menerima scan barcode: alat scanner "mengetik" kode lalu otomatis
        // menekan Enter/Done, jadi tinggal dengarkan aksi itu di kotak search yang sama.
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                String teks = etSearch.getText().toString().trim();

                // Heuristik sederhana: barcode isinya cuma angka, beda dari nama produk yang biasanya
                // ada hurufnya. Kalau yang di-scan/diketik angka semua, coba cocokkan sebagai barcode.
                // Kalau bukan (user cuma ngetik nama produk lalu pencet Done), jangan dianggap scan gagal,
                // cukup tutup keyboard saja dan biarkan hasil pencarian nama tampil seperti biasa.
                if (!teks.isEmpty() && teks.matches("\\d+")) {
                    prosesBarcodeScan(teks);
                }
                sembunyikanKeyboard();
                return true;
            }
            return false;
        });

        btnReset.setOnClickListener(v -> resetForm());
        btnBayar.setOnClickListener(v -> prosesBayar());

        // Hitung ulang otomatis setiap diskon diketik
        etDiskon.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                hitungOtomatis();
            }
            @Override public void afterTextChanged(Editable s) {}
        });


        // Hitung kembalian otomatis setiap kali jumlah bayar diketik
        etJumlahBayar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                hitungKembalian();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void sembunyikanKeyboard() {
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    private void prosesBarcodeScan(String barcode) {
        if (barcode.isEmpty()) return;

        Produk produkDitemukan = null;
        for (Produk p : daftarProdukAsli) {
            if (barcode.equals(p.getBarcode())) {
                produkDitemukan = p;
                break;
            }
        }

        if (produkDitemukan != null) {
            tambahKeKeranjang(produkDitemukan); // fungsi yang sudah ada, otomatis cek stok/expired juga
            Toast.makeText(requireContext(), "✓ " + produkDitemukan.getNama() + " ditambahkan", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Barcode tidak ditemukan: " + barcode, Toast.LENGTH_SHORT).show();
        }

        // Bersihkan kotak search, siapkan buat scan barang berikutnya (otomatis nampilin semua produk lagi)
        etSearch.setText("");
        etSearch.requestFocus();
    }

    // Produk normal (masih bisa dijual) tampil duluan, produk kadaluarsa/stok habis digeser ke bawah.
    // Pakai List.sort (stable sort) supaya urutan asli di dalam masing-masing kelompok tetap terjaga.
    private void urutkanBerdasarkanKetersediaan(List<Produk> daftar) {
        daftar.sort((a, b) -> Boolean.compare(a.isTidakBisaDijual(), b.isTidakBisaDijual()));
    }

    // Menyaring daftar produk berdasarkan kata kunci pencarian
    private void filterProduk(String keyword) {
        List<Produk> hasil = new ArrayList<>();
        for (Produk p : daftarProdukAsli) {
            if (p.getNama().toLowerCase().contains(keyword.toLowerCase())) {
                hasil.add(p);
            }
        }
        urutkanBerdasarkanKetersediaan(hasil);
        daftarProdukTampil.clear();
        daftarProdukTampil.addAll(hasil);
        produkAdapter.notifyDataSetChanged();
    }
    private void buatKategoriChips() {
        List<String> kategoriList = new ArrayList<>();
        kategoriList.add("Semua");
        for (Produk p : daftarProdukAsli) {
            String jenis = p.getNamaJenis();
            if (jenis != null && !kategoriList.contains(jenis)) {
                kategoriList.add(jenis);
            }
        }

        llKategori.removeAllViews();
        for (String kategori : kategoriList) {
            TextView chip = new TextView(requireContext());
            chip.setText(kategori);
            chip.setTextSize(12);
            chip.setPadding(36, 16, 36, 16);
            chip.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(8);
            chip.setLayoutParams(params);

            boolean aktif = kategori.equals(kategoriDipilih);
            chip.setBackgroundResource(aktif ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(aktif ? 0xFFFFFFFF : 0xFF1A1A2E);

            chip.setOnClickListener(v -> {
                kategoriDipilih = kategori;
                buatKategoriChips();
                terapkanFilter();
            });

            llKategori.addView(chip);
        }
    }

    private void terapkanFilter() {
        String keyword = etSearch.getText().toString().toLowerCase();
        List<Produk> hasil = new ArrayList<>();

        for (Produk p : daftarProdukAsli) {
            boolean cocokKeyword = p.getNama().toLowerCase().contains(keyword);
            boolean cocokKategori = kategoriDipilih.equals("Semua")
                    || kategoriDipilih.equals(p.getNamaJenis());

            if (cocokKeyword && cocokKategori) {
                hasil.add(p);
            }
        }

        urutkanBerdasarkanKetersediaan(hasil);
        daftarProdukTampil.clear();
        daftarProdukTampil.addAll(hasil);
        produkAdapter.notifyDataSetChanged();
    }

    // DIUBAH: sekarang cocokkan produk berdasarkan kodeProduk, bukan nama
    // (lebih akurat, karena nama produk bisa saja mirip/duplikat)
    private void tambahKeKeranjang(Produk produk) {
        if (produk.isTidakBisaDijual()) {
            Toast.makeText(requireContext(), produk.getNama() + " tidak bisa dijual ("
                            + produk.getLabelTidakBisaDijual().toLowerCase() + ")",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Cek apakah produk ini sudah ada di keranjang
        for (ItemKeranjang item : daftarKeranjang) {
            if (item.getProduk().getKodeProduk() == produk.getKodeProduk()) {
                // Cegah qty melebihi stok yang tersedia
                if (item.getJumlah() + 1 > produk.getStok()) {
                    Toast.makeText(requireContext(), "Stok " + produk.getNama() + " tidak mencukupi (sisa "
                            + produk.getStok() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
                item.setJumlah(item.getJumlah() + 1);
                keranjangAdapter.notifyDataSetChanged();
                cekKeranjangKosong();
                hitungOtomatis();
                return;
            }
        }

        // Produk belum ada di keranjang, cek dulu stoknya minimal 1
        if (produk.getStok() < 1) {
            Toast.makeText(requireContext(), "Stok " + produk.getNama() + " tidak mencukupi", Toast.LENGTH_SHORT).show();
            return;
        }

        daftarKeranjang.add(new ItemKeranjang(produk, 1));
        keranjangAdapter.notifyItemInserted(daftarKeranjang.size() - 1);
        cekKeranjangKosong();
        hitungOtomatis();
    }

    // Menampilkan teks "Keranjang masih kosong" jika belum ada item
    private void cekKeranjangKosong() {
        if (daftarKeranjang.isEmpty()) {
            tvKeranjangKosong.setVisibility(View.VISIBLE);
            rvKeranjang.setVisibility(View.GONE);
        } else {
            tvKeranjangKosong.setVisibility(View.GONE);
            rvKeranjang.setVisibility(View.VISIBLE);
        }
    }

    // Dipanggil otomatis setiap kali keranjang, diskon, atau member berubah
    private void hitungOtomatis() {
        // Jika keranjang kosong, tampilkan Rp 0 semua
        if (daftarKeranjang.isEmpty()) {
            tvSubtotal.setText(formatRupiah(0));
            tvDiskon.setText("- " + formatRupiah(0));
            tvTotalBayar.setText(formatRupiah(0));
            totalBayar = 0;
            hitungKembalian();
            return;
        }

        // Hitung subtotal dari seluruh item di keranjang
        double subtotal = 0;
        for (ItemKeranjang item : daftarKeranjang) {
            subtotal += item.getSubtotalItem();
        }

        // Hitung diskon: kosong = 0%, isi = 1-100%
        double pctDiskon = 0;
        String sd = etDiskon.getText().toString().trim();
        if (!sd.isEmpty()) {
            int diskon = Integer.parseInt(sd);
            if (diskon < 1 || diskon > 100) {
                etDiskon.setError("Diskon harus antara 1 - 100!");
                // Tetap hitung dengan diskon 0 agar tampilan tidak rusak
                pctDiskon = 0;
            } else {
                pctDiskon = diskon / 100.0;
            }
        }


        double nilaiDiskon = subtotal * pctDiskon;
        totalBayar = subtotal - nilaiDiskon;

        tvSubtotal.setText(formatRupiah(subtotal));
        tvDiskon.setText("- " + formatRupiah(nilaiDiskon));
        tvTotalBayar.setText(formatRupiah(totalBayar));

        // Hitung ulang kembalian setiap total berubah
        hitungKembalian();
    }

    // Dipanggil setiap kali field Jumlah Bayar berubah atau total berubah
    private void hitungKembalian() {
        String teks = etJumlahBayar.getText().toString().trim();
        if (teks.isEmpty()) {
            tvKembalian.setText("Rp 0");
            return;
        }

        double jumlahBayar = Double.parseDouble(teks);
        double kembalian = jumlahBayar - totalBayar;

        if (kembalian < 0) {
            // Uang yang dibayar kurang dari total
            tvKembalian.setText("Kurang " + formatRupiah(Math.abs(kembalian)));
        } else {
            tvKembalian.setText(formatRupiah(kembalian));
        }
    }

    // DIUBAH: sekarang cuma validasi lalu tampilkan dialog konfirmasi dulu
    private void prosesBayar() {
        if (daftarKeranjang.isEmpty()) {
            Toast.makeText(requireContext(), "Keranjang masih kosong, pilih produk dahulu!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String sJumlahBayar = etJumlahBayar.getText().toString().trim();
        if (sJumlahBayar.isEmpty()) {
            etJumlahBayar.setError("Jumlah bayar tidak boleh kosong!");
            etJumlahBayar.requestFocus();
            return;
        }

        double jumlahBayar = Double.parseDouble(sJumlahBayar);

        if (jumlahBayar < totalBayar) {
            etJumlahBayar.setError("Jumlah bayar kurang dari total!");
            etJumlahBayar.requestFocus();
            return;
        }

        double kembalian = jumlahBayar - totalBayar;
        tampilkanDialogKonfirmasi(jumlahBayar, kembalian);
    }

    // BARU: dialog konfirmasi sebelum transaksi diproses ke server
    private void tampilkanDialogKonfirmasi(double jumlahBayar, double kembalian) {
        Context ctx = requireContext();
        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_konfirmasi_transaksi, null);

        TextView tvJudulDetailProduk = dialogView.findViewById(R.id.tvJudulDetailProduk);
        LinearLayout llDaftarItem = dialogView.findViewById(R.id.llDaftarItemKonfirmasi);
        TextView tvKonfirmasiTotal = dialogView.findViewById(R.id.tvKonfirmasiTotal);
        TextView tvKonfirmasiBayar = dialogView.findViewById(R.id.tvKonfirmasiBayar);
        TextView tvKonfirmasiKembali = dialogView.findViewById(R.id.tvKonfirmasiKembali);
        Button btnProsesTransaksi = dialogView.findViewById(R.id.btnProsesTransaksi);
        Button btnBatalKonfirmasi = dialogView.findViewById(R.id.btnBatalKonfirmasi);

        tvJudulDetailProduk.setText("🛒 Detail Produk (" + daftarKeranjang.size() + " item)");
        tvKonfirmasiTotal.setText(formatRupiah(totalBayar));
        tvKonfirmasiBayar.setText(formatRupiah(jumlahBayar));
        tvKonfirmasiKembali.setText(formatRupiah(kembalian));

        NumberFormat fmt = NumberFormat.getInstance(new Locale("id", "ID"));
        llDaftarItem.removeAllViews();
        for (ItemKeranjang item : daftarKeranjang) {
            View itemView = LayoutInflater.from(ctx).inflate(R.layout.item_konfirmasi_produk, llDaftarItem, false);
            TextView tvNama = itemView.findViewById(R.id.tvNamaProdukKonfirmasi);
            TextView tvQtyHarga = itemView.findViewById(R.id.tvQtyHargaKonfirmasi);
            TextView tvSubtotalItem = itemView.findViewById(R.id.tvSubtotalProdukKonfirmasi);

            tvNama.setText(item.getProduk().getNama());
            tvQtyHarga.setText(item.getJumlah() + " x Rp " + fmt.format(item.getProduk().getHarga()));
            tvSubtotalItem.setText("Rp " + fmt.format(item.getSubtotalItem()));
            llDaftarItem.addView(itemView);
        }

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnProsesTransaksi.setOnClickListener(v -> {
            dialog.dismiss();
            kirimTransaksiKeServer(jumlahBayar, kembalian);
        });
        btnBatalKonfirmasi.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // BARU: baru di sini transaksi beneran dikirim ke API, dipanggil setelah user tekan "Proses Transaksi"
    private void kirimTransaksiKeServer(double jumlahBayar, double kembalian) {
        List<CartItem> cartItems = new ArrayList<>();
        for (ItemKeranjang item : daftarKeranjang) {
            cartItems.add(new CartItem(
                    item.getProduk().getKodeProduk(),
                    item.getJumlah(),
                    item.getProduk().getHarga()
            ));
        }

        TransaksiRequest request = new TransaksiRequest(cartItems, totalBayar, jumlahBayar, kembalian);

        btnBayar.setEnabled(false);
        btnBayar.setText("Memproses...");

        apiService.checkout(sessionManager.getBearerToken(), request)
                .enqueue(new Callback<TransaksiResponse>() {
                    @Override
                    public void onResponse(Call<TransaksiResponse> call, Response<TransaksiResponse> response) {
                        if (!isAdded()) return;
                        btnBayar.setEnabled(true);
                        btnBayar.setText("Bayar");

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            tampilkanDialogSukses(response.body().getKodeTransaksi(), jumlahBayar, kembalian);
                        } else {
                            String pesan = "Transaksi gagal diproses";
                            if (response.body() != null && response.body().getMessage() != null) {
                                pesan = response.body().getMessage();
                            } else if (response.errorBody() != null) {
                                try {
                                    pesan = response.errorBody().string();
                                } catch (Exception ignored) {}
                            }
                            Toast.makeText(requireContext(), pesan, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<TransaksiResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        btnBayar.setEnabled(true);
                        btnBayar.setText("Bayar");
                        Toast.makeText(requireContext(),
                                "Gagal terhubung ke server: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Dialog "Pembayaran Berhasil", sekarang tombol Cetak Nota beneran nyambung ke printer
    private void tampilkanDialogSukses(String kodeTransaksi, double jumlahBayar, double kembalian) {
        Context ctx = requireContext();
        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_pembayaran_berhasil, null);

        TextView tvKode = dialogView.findViewById(R.id.tvKodeTransaksiSukses);
        Button btnCetakNota = dialogView.findViewById(R.id.btnCetakNota);
        Button btnTransaksiBaru = dialogView.findViewById(R.id.btnTransaksiBaru);

        tvKode.setText(kodeTransaksi);

        // Susun data struk dari keranjang yang masih ada di memori (belum di-reset)
        StrukData data = new StrukData();
        data.kodeTransaksi = kodeTransaksi;
        data.namaKasir = sessionManager.getNamaLengkap();
        data.waktu = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        data.shift = sessionManager.getShift();
        data.total = totalBayar;
        data.bayar = jumlahBayar;
        data.kembalian = kembalian;
        data.items = new ArrayList<>();
        for (ItemKeranjang item : daftarKeranjang) {
            data.items.add(new StrukItem(
                    item.getProduk().getNama(),
                    item.getJumlah(),
                    item.getProduk().getHarga(),
                    item.getSubtotalItem()
            ));
        }

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnCetakNota.setOnClickListener(v -> NotaPreviewHelper.tampilkan(ctx, data, () -> mintaIzinLaluCetak(data)));

        btnTransaksiBaru.setOnClickListener(v -> {
            dialog.dismiss();
            resetForm();
            siapkanDataProduk();
        });

        dialog.show();
    }

    // Cek izin Bluetooth dulu sebelum benar-benar cetak; kalau belum ada, minta izin dulu
    private void mintaIzinLaluCetak(StrukData data) {
        if (PrinterHelper.izinBluetoothSudahAda(requireContext())) {
            PrinterHelper.pilihPrinterDanCetak(requireContext(), data, printCallback);
        } else {
            strukMenunggu = data;
            izinBluetoothLauncher.launch(PrinterHelper.daftarIzinBluetoothPerluDiminta());
        }
    }

    private void resetForm() {
        daftarKeranjang.clear();
        keranjangAdapter.notifyDataSetChanged();
        cekKeranjangKosong();

        etSearch.setText("");
        etDiskon.setText("");
        etJumlahBayar.setText("");

        hitungOtomatis();

        Toast.makeText(requireContext(), "Form berhasil direset", Toast.LENGTH_SHORT).show();
    }

    private String formatRupiah(double angka) {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("id", "ID"));
        return "Rp " + fmt.format((long) angka);
    }
}
