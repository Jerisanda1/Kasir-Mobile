package com.example.program_kasir;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import android.view.Gravity;
import android.widget.LinearLayout;


public class TransaksiActivity extends AppCompatActivity {

    // Daftar lengkap produk yang dijual toko (diisi dari API)
    private final List<Produk> daftarProdukAsli = new ArrayList<>();
    // Daftar produk yang sedang ditampilkan (hasil filter search)
    private final List<Produk> daftarProdukTampil = new ArrayList<>();
    // Daftar isi keranjang belanja
    private final List<ItemKeranjang> daftarKeranjang = new ArrayList<>();

    private RecyclerView rvProduk, rvKeranjang;
    private ProdukAdapter produkAdapter;
    private KeranjangAdapter keranjangAdapter;

    private EditText etSearch, etDiskon, etJumlahBayar;

    private TextView tvSubtotal, tvDiskon, tvTotalBayar, tvKembalian, tvTanggal, tvKeranjangKosong;
    private Button btnReset, btnBayar, ivLogout;
    private TextView tvNamaKasir;
    private LinearLayout llKategori;
    private String kategoriDipilih = "Semua";
    private double totalBayar = 0;

    // TAMBAHAN UNTUK API
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaksi);

        inisialisasiView();

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        tampilkanInfoKasir();
        siapkanDataProduk();
        setupRecyclerView();
        setupListener();

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        tvTanggal.setText(sdf.format(new java.util.Date()));

        // Tampilkan kondisi awal Rp 0
        hitungOtomatis();
    }

    private void inisialisasiView() {
        rvProduk          = findViewById(R.id.rvProduk);
        rvKeranjang       = findViewById(R.id.rvKeranjang);
        etSearch          = findViewById(R.id.etSearch);
        etDiskon          = findViewById(R.id.etDiskon);
        etJumlahBayar     = findViewById(R.id.etJumlahBayar);
        tvSubtotal        = findViewById(R.id.tvSubtotal);
        tvDiskon          = findViewById(R.id.tvDiskon);
        tvTotalBayar      = findViewById(R.id.tvTotalBayar);
        tvKembalian       = findViewById(R.id.tvKembalian);
        tvTanggal         = findViewById(R.id.tvTanggal);
        tvKeranjangKosong = findViewById(R.id.tvKeranjangKosong);
        btnReset          = findViewById(R.id.btnReset);
        btnBayar          = findViewById(R.id.btnBayar);
        ivLogout          = findViewById(R.id.ivLogout);
        tvNamaKasir       = findViewById(R.id.tvNamaKasir);
        llKategori = findViewById(R.id.llKategori);
    }

    // BARU: susun teks "Nama (Role - Shift jam)" di header
    private void tampilkanInfoKasir() {
        String nama  = sessionManager.getNamaLengkap();
        String level = sessionManager.getLevel();
        String shift = sessionManager.getShift();

        String levelLabel = "kasir".equalsIgnoreCase(level) ? "Kasir" : "Admin";
        String teks;

        if (shift != null && !shift.isEmpty()) {
            String jamShift = "1".equals(shift) ? "07:00-15:00" : "15:00-23:00";
            teks = nama + " (" + levelLabel + " - Shift " + shift + " (" + jamShift + "))";
        } else {
            teks = nama + " (" + levelLabel + ")";
        }

        tvNamaKasir.setText(teks);
    }

    // DIUBAH: sebelumnya data dummy, sekarang fetch dari API
    private void siapkanDataProduk() {
        apiService.getProduk(sessionManager.getBearerToken(), null)
                .enqueue(new Callback<ProdukResponse>() {
                    @Override
                    public void onResponse(Call<ProdukResponse> call, Response<ProdukResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            daftarProdukAsli.clear();
                            daftarProdukAsli.addAll(response.body().getData());

                            daftarProdukTampil.clear();
                            daftarProdukTampil.addAll(daftarProdukAsli);

                            if (produkAdapter != null) {
                                produkAdapter.notifyDataSetChanged();
                            }
                            buatKategoriChips();
                        } else {
                            Toast.makeText(TransaksiActivity.this,
                                    "Gagal mengambil data produk dari server", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ProdukResponse> call, Throwable t) {
                        Toast.makeText(TransaksiActivity.this,
                                "Tidak bisa terhubung ke server: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupRecyclerView() {
        // Grid produk 3 kolom
        rvProduk.setLayoutManager(new GridLayoutManager(this, 3));
        produkAdapter = new ProdukAdapter(daftarProdukTampil, this::tambahKeKeranjang);
        rvProduk.setAdapter(produkAdapter);

        // List keranjang vertikal, dibatasi tinggi via XML (180dp) agar muncul scroll
        rvKeranjang.setLayoutManager(new LinearLayoutManager(this));
        keranjangAdapter = new KeranjangAdapter(daftarKeranjang, new KeranjangAdapter.OnKeranjangActionListener() {
            @Override
            public void onTambahQty(int position) {
                ItemKeranjang item = daftarKeranjang.get(position);

                // Cegah qty melebihi stok yang tersedia
                if (item.getJumlah() + 1 > item.getProduk().getStok()) {
                    Toast.makeText(TransaksiActivity.this,
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
        // Search produk secara realtime
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProduk(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnReset.setOnClickListener(v -> resetForm());
        btnBayar.setOnClickListener(v -> prosesBayar());
        ivLogout.setOnClickListener(v -> konfirmasiLogout());

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

    // Menyaring daftar produk berdasarkan kata kunci pencarian
    private void filterProduk(String keyword) {
        List<Produk> hasil = new ArrayList<>();
        for (Produk p : daftarProdukAsli) {
            if (p.getNama().toLowerCase().contains(keyword.toLowerCase())) {
                hasil.add(p);
            }
        }
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
            TextView chip = new TextView(this);
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

        daftarProdukTampil.clear();
        daftarProdukTampil.addAll(hasil);
        produkAdapter.notifyDataSetChanged();
    }

    // DIUBAH: sekarang cocokkan produk berdasarkan kodeProduk, bukan nama
    // (lebih akurat, karena nama produk bisa saja mirip/duplikat)
    private void tambahKeKeranjang(Produk produk) {
        if (produk.isTidakBisaDijual()) {
            Toast.makeText(this, produk.getNama() + " tidak bisa dijual ("
                            + produk.getLabelTidakBisaDijual().toLowerCase() + ")",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Cek apakah produk ini sudah ada di keranjang
        for (ItemKeranjang item : daftarKeranjang) {
            if (item.getProduk().getKodeProduk() == produk.getKodeProduk()) {
                // Cegah qty melebihi stok yang tersedia
                if (item.getJumlah() + 1 > produk.getStok()) {
                    Toast.makeText(this, "Stok " + produk.getNama() + " tidak mencukupi (sisa "
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
            Toast.makeText(this, "Stok " + produk.getNama() + " tidak mencukupi", Toast.LENGTH_SHORT).show();
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
            tvKeranjangKosong.setVisibility(android.view.View.VISIBLE);
            rvKeranjang.setVisibility(android.view.View.GONE);
        } else {
            tvKeranjangKosong.setVisibility(android.view.View.GONE);
            rvKeranjang.setVisibility(android.view.View.VISIBLE);
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

    // DIUBAH TOTAL: sebelumnya cuma nampilin dialog, sekarang beneran kirim ke API
    private void prosesBayar() {
        // Validasi: keranjang tidak boleh kosong
        if (daftarKeranjang.isEmpty()) {
            Toast.makeText(this, "Keranjang masih kosong, pilih produk dahulu!",
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

        // Validasi: uang bayar tidak boleh kurang dari total
        if (jumlahBayar < totalBayar) {
            etJumlahBayar.setError("Jumlah bayar kurang dari total!");
            etJumlahBayar.requestFocus();
            return;
        }

        double kembalian = jumlahBayar - totalBayar;

        // Susun cart untuk dikirim ke server
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
                        btnBayar.setEnabled(true);
                        btnBayar.setText("Bayar");

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            tampilkanStrukSukses(response.body().getKodeTransaksi(), jumlahBayar, kembalian);
                        } else {
                            String pesan = "Transaksi gagal diproses";
                            if (response.body() != null && response.body().getMessage() != null) {
                                pesan = response.body().getMessage();
                            } else if (response.errorBody() != null) {
                                try {
                                    pesan = response.errorBody().string();
                                } catch (Exception ignored) {}
                            }
                            Toast.makeText(TransaksiActivity.this, pesan, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<TransaksiResponse> call, Throwable t) {
                        btnBayar.setEnabled(true);
                        btnBayar.setText("Bayar");
                        Toast.makeText(TransaksiActivity.this,
                                "Gagal terhubung ke server: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // BARU: dialog struk sukses, dipanggil setelah server konfirmasi transaksi tersimpan
    private void tampilkanStrukSukses(String kodeTransaksi, double jumlahBayar, double kembalian) {
        StringBuilder daftarBelanja = new StringBuilder();
        for (ItemKeranjang item : daftarKeranjang) {
            daftarBelanja.append(item.getProduk().getNama())
                    .append(" x").append(item.getJumlah())
                    .append(" = ").append(formatRupiah(item.getSubtotalItem()))
                    .append("\n");
        }

        String pesan = "No. Transaksi: " + kodeTransaksi + "\n\n"
                + daftarBelanja.toString()
                + "\nSubtotal : " + tvSubtotal.getText()
                + "\nDiskon   : " + tvDiskon.getText()
                + "\nTOTAL    : " + tvTotalBayar.getText()
                + "\nDibayar  : " + formatRupiah(jumlahBayar)
                + "\nKembalian: " + formatRupiah(kembalian);

        new AlertDialog.Builder(this)
                .setTitle("Transaksi Berhasil!")
                .setMessage(pesan)
                .setPositiveButton("Transaksi Baru", (d, w) -> {
                    resetForm();
                    siapkanDataProduk(); // refresh stok terbaru dari server
                })
                .setCancelable(false)
                .show();
    }

    private void resetForm() {
        daftarKeranjang.clear();
        keranjangAdapter.notifyDataSetChanged();
        cekKeranjangKosong();

        etSearch.setText("");
        etDiskon.setText("");
        etJumlahBayar.setText("");

        hitungOtomatis();

        Toast.makeText(this, "Form berhasil direset", Toast.LENGTH_SHORT).show();
    }

    private void konfirmasiLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Apakah Anda yakin ingin keluar?")
                .setPositiveButton("Ya, Keluar", (d, w) -> {
                    sessionManager.clearSession();
                    Intent intent = new Intent(TransaksiActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private String formatRupiah(double angka) {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("id", "ID"));
        return "Rp " + fmt.format((long) angka);
    }
}