package com.example.program_kasir;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.program_kasir.api.ApiClient;
import com.example.program_kasir.api.ApiService;

// Host tunggal untuk halaman Transaksi & Riwayat.
// Sidebar (logo, menu, tombol logout) di sini bersifat PERSISTEN alias tidak ikut
// dibuat ulang saat pindah menu -- yang berpindah cuma isi kolom kanan (fragment)
// plus kotak indikator ungu yang digeser (translationY) ke menu yang baru diklik.
public class MainActivity extends AppCompatActivity {

    private enum Menu { TRANSAKSI, RIWAYAT }

    private LinearLayout llMenuTransaksi, llMenuRiwayat, llTutupShift;
    private TextView tvLabelTransaksi, tvLabelRiwayat;
    private ImageView ivIconTransaksi, ivIconRiwayat;
    private View vIndikatorAktif;
    private Button btnLogout;

    private SessionManager sessionManager;
    private ApiService apiService;
    private Menu menuAktif = Menu.TRANSAKSI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        llMenuTransaksi  = findViewById(R.id.llMenuTransaksi);
        llMenuRiwayat    = findViewById(R.id.llMenuRiwayat);
        tvLabelTransaksi = findViewById(R.id.tvLabelTransaksi);
        ivIconTransaksi = findViewById(R.id.ivIconTransaksi);
        ivIconRiwayat   = findViewById(R.id.ivIconRiwayat);
        tvLabelRiwayat   = findViewById(R.id.tvLabelRiwayat);
        vIndikatorAktif  = findViewById(R.id.vIndikatorAktif);
        btnLogout        = findViewById(R.id.ivLogout);
        llTutupShift     = findViewById(R.id.llTutupShift);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        llMenuTransaksi.setOnClickListener(v -> pindahKeMenu(Menu.TRANSAKSI));
        llMenuRiwayat.setOnClickListener(v -> pindahKeMenu(Menu.RIWAYAT));
        btnLogout.setOnClickListener(v -> konfirmasiLogout());
        llTutupShift.setOnClickListener(v -> ShiftKasHelper.mulaiTutupShift(this, apiService, sessionManager, () -> {
            // Tutup shift = sesi kerja kasir ini selesai -> otomatis logout, mulai bersih di shift berikutnya
            sessionManager.clearSession();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }));

        // Gerbang wajib: kalau kasir belum buka shift hari ini, dialog non-cancelable
        // muncul menutupi konten sampai modal awal diisi. Dipanggil sekali di sini saja.
        ShiftKasHelper.pastikanShiftSiap(this, apiService, sessionManager, () -> {
            // Sudah siap (shift aktif) -- konten di belakang dialog memang sudah dipersiapkan
            // di bawah, jadi tidak perlu ada yang dilakukan tambahan di sini.
        });

        // Tentukan fragment awal (atau pulihkan yang sudah ada kalau Activity di-recreate)
        Fragment fragmentSaatIni = getSupportFragmentManager().findFragmentById(R.id.frameContent);
        if (fragmentSaatIni == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameContent, new TransaksiFragment())
                    .commit();
            menuAktif = Menu.TRANSAKSI;
        } else {
            menuAktif = (fragmentSaatIni instanceof RiwayatFragment) ? Menu.RIWAYAT : Menu.TRANSAKSI;
        }

        // Posisikan kotak indikator SETELAH sidebar selesai diukur (baru getTop()/getHeight() valid)
        View menuAwal = (menuAktif == Menu.RIWAYAT) ? llMenuRiwayat : llMenuTransaksi;
        menuAwal.post(() -> {
            vIndikatorAktif.setTranslationY(menuAwal.getTop());
            ViewGroup.LayoutParams lp = vIndikatorAktif.getLayoutParams();
            lp.height = menuAwal.getHeight();
            vIndikatorAktif.setLayoutParams(lp);
        });
        perbaruiLabelAktif(menuAktif);

        // Tombol Back: kalau lagi di Riwayat -> balik ke Transaksi (bukan keluar app)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (menuAktif == Menu.RIWAYAT) {
                    pindahKeMenu(Menu.TRANSAKSI);
                } else {
                    finish();
                }
            }
        });
    }

    // Pindah menu: geser indikator ke posisi baru + crossfade konten, tanpa slide kiri-kanan
    private void pindahKeMenu(Menu target) {
        if (target == menuAktif) return;

        View viewTarget = (target == Menu.TRANSAKSI) ? llMenuTransaksi : llMenuRiwayat;
        geserIndikatorKe(viewTarget);
        perbaruiLabelAktif(target);

        Fragment fragmentBaru = (target == Menu.TRANSAKSI) ? new TransaksiFragment() : new RiwayatFragment();
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.frameContent, fragmentBaru)
                .commit();

        menuAktif = target;
    }

    // Geser kotak ungu (indikator aktif) ke posisi menu yang baru diklik
    private void geserIndikatorKe(View target) {
        vIndikatorAktif.animate()
                .translationY(target.getTop())
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        ViewGroup.LayoutParams lp = vIndikatorAktif.getLayoutParams();
        lp.height = target.getHeight();
        vIndikatorAktif.setLayoutParams(lp);
    }

    private void perbaruiLabelAktif(Menu target) {

        boolean transaksiAktif = target == Menu.TRANSAKSI;
        boolean riwayatAktif = target == Menu.RIWAYAT;

        tvLabelTransaksi.setTextColor(
                transaksiAktif ? 0xFFFFFFFF : 0xFF1A1A2E);
        tvLabelTransaksi.setTypeface(
                null, transaksiAktif ? Typeface.BOLD : Typeface.NORMAL);

        tvLabelRiwayat.setTextColor(
                riwayatAktif ? 0xFFFFFFFF : 0xFF1A1A2E);
        tvLabelRiwayat.setTypeface(
                null, riwayatAktif ? Typeface.BOLD : Typeface.NORMAL);

        ivIconTransaksi.setColorFilter(
                transaksiAktif ? 0xFFFFFFFF : 0xFF1A1A2E);

        ivIconRiwayat.setColorFilter(
                riwayatAktif ? 0xFFFFFFFF : 0xFF1A1A2E);
    }

    private void konfirmasiLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Apakah Anda yakin ingin keluar?")
                .setPositiveButton("Ya, Keluar", (d, w) -> {
                    sessionManager.clearSession();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}