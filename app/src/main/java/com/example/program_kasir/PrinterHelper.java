package com.example.program_kasir;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Helper untuk mencetak struk ke printer thermal Bluetooth (protokol ESC/POS),
 * pakai library DantSu/ESCPOS-ThermalPrinter-Android.
 *
 * Desain teks struknya niru persis nota di web kasir: header toko, garis putus-putus,
 * daftar item, total/bayar/kembali, lalu footer ucapan terima kasih.
 *
 * CATATAN: nama method di library (EscPosPrinter, BluetoothConnection, dst) sebaiknya
 * dicek ulang lewat auto-complete Android Studio setelah dependency ke-download,
 * karena versi library bisa saja sedikit beda dari yang diasumsikan di sini.
 */
public class PrinterHelper {

    private static final String PREF_NAME = "printer_prefs";
    private static final String KEY_MAC_PRINTER = "mac_printer_tersimpan";

    // Info toko, sama seperti yang tertulis di nota web (print_nota.php)
    private static final String NAMA_TOKO   = "JIMBO MART";
    private static final String ALAMAT_TOKO = "Jl. Bukit Jimbaran No. 23";
    private static final String TELEPON_TOKO = "Telp: 0857-9235-8465";

    public interface PrintCallback {
        void onSukses();
        void onGagal(String pesanError);
    }

    // Android 12 (API 31) ke atas WAJIB izin runtime BLUETOOTH_CONNECT sebelum baca daftar
    // perangkat Bluetooth atau konek ke printer. Di bawah itu, cukup izin di Manifest saja.
    public static boolean izinBluetoothSudahAda(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // Titik masuk utama: cari printer yang sudah dipasangkan (paired) di tablet, lalu cetak ke situ.
    // Kalau cuma ada 1 printer / sudah pernah dipilih sebelumnya, langsung cetak tanpa nanya lagi.
    public static void pilihPrinterDanCetak(Context context, StrukData data, PrintCallback callback) {
        if (!izinBluetoothSudahAda(context)) {
            callback.onGagal("Izin Bluetooth belum diberikan");
            return;
        }

        // Semua pemanggilan API Bluetooth di bawah ini (isEnabled, getBondedDevices, getName)
        // butuh izin BLUETOOTH_CONNECT di Android 12+. Sudah dicek lewat izinBluetoothSudahAda()
        // di atas, tapi lint Android Studio tidak bisa "melacak" pengecekan lewat method terpisah
        // seperti itu -- makanya tetap dibungkus try-catch(SecurityException) di sini sebagai
        // jaring pengaman eksplisit (juga berguna kalau user mencabut izin tepat di tengah proses).
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                callback.onGagal("Tablet ini tidak punya Bluetooth");
                return;
            }
            if (!adapter.isEnabled()) {
                callback.onGagal("Bluetooth tablet belum aktif, aktifkan dulu lalu coba lagi");
                return;
            }

            Set<BluetoothDevice> perangkatTerpasang = adapter.getBondedDevices();
            if (perangkatTerpasang.isEmpty()) {
                callback.onGagal("Belum ada printer yang dipasangkan. Pasangkan dulu lewat Pengaturan > Bluetooth di tablet.");
                return;
            }

            SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String macTersimpan = pref.getString(KEY_MAC_PRINTER, null);

            // Sudah pernah pilih printer sebelumnya & printer itu masih terpasang -> langsung cetak
            if (macTersimpan != null) {
                for (BluetoothDevice device : perangkatTerpasang) {
                    if (device.getAddress().equals(macTersimpan)) {
                        cetakKePerangkat(device, data, callback);
                        return;
                    }
                }
            }

            // Cuma ada 1 perangkat terpasang -> anggap itu printernya, simpan sebagai default
            if (perangkatTerpasang.size() == 1) {
                BluetoothDevice satuSatunya = perangkatTerpasang.iterator().next();
                pref.edit().putString(KEY_MAC_PRINTER, satuSatunya.getAddress()).apply();
                cetakKePerangkat(satuSatunya, data, callback);
                return;
            }

            // Ada beberapa perangkat -> biarkan kasir pilih sendiri mana yang printer
            List<BluetoothDevice> daftarDevice = new ArrayList<>(perangkatTerpasang);
            String[] namaDevice = new String[daftarDevice.size()];
            for (int i = 0; i < daftarDevice.size(); i++) {
                String nama = daftarDevice.get(i).getName();
                // getName() bisa null untuk beberapa perangkat BLE, jaga-jaga biar gak nampilin "null"
                namaDevice[i] = (nama != null) ? nama : daftarDevice.get(i).getAddress();
            }

            new AlertDialog.Builder(context)
                    .setTitle("Pilih Printer")
                    .setItems(namaDevice, (dialog, which) -> {
                        BluetoothDevice dipilih = daftarDevice.get(which);
                        pref.edit().putString(KEY_MAC_PRINTER, dipilih.getAddress()).apply();
                        cetakKePerangkat(dipilih, data, callback);
                    })
                    .setNegativeButton("Batal", null)
                    .show();

        } catch (SecurityException e) {
            callback.onGagal("Izin Bluetooth ditolak sistem, coba beri izin ulang lewat Pengaturan aplikasi");
        }
    }

    // Kalau nanti mau ganti ke printer lain (bukan yang biasa dipakai), panggil ini dulu
    public static void lupakanPrinterTersimpan(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().remove(KEY_MAC_PRINTER).apply();
    }

    // Koneksi & proses cetak ke printer itu sendiri BUKAN pekerjaan ringan (I/O Bluetooth),
    // jadi wajib dijalankan di background thread, bukan di main/UI thread.
    private static void cetakKePerangkat(BluetoothDevice device, StrukData data, PrintCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            try {
                BluetoothConnection connection = new BluetoothConnection(device);
                // 203 = dpi standar printer thermal, 48f = lebar cetak (mm) utk kertas 58mm,
                // 32 = perkiraan jumlah karakter per baris pada lebar itu
                EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);
                printer.printFormattedText(bangunTeksStruk(data));

                mainHandler.post(() -> {
                    if (callback != null) callback.onSukses();
                });
            } catch (Exception e) {
                String pesanError = e.getMessage() != null ? e.getMessage() : "Gagal cetak, cek koneksi printer";
                mainHandler.post(() -> {
                    if (callback != null) callback.onGagal(pesanError);
                });
            }
        }).start();
    }

    // Susun teks struk pakai format markup dari library:
    // [L]/[C]/[R] = rata kiri/tengah/kanan, <b>...</b> = tebal.
    // Desainnya niru urutan di print_nota.php: header toko -> info transaksi -> daftar item -> total -> footer.
    private static String bangunTeksStruk(StrukData data) {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("id", "ID"));
        // 32 strip, pas 1 baris penuh di printer 58mm (32 karakter/baris)
        String garis = "--------------------------------";

        StringBuilder sb = new StringBuilder();

        sb.append("[C]<b>").append(NAMA_TOKO).append("</b>\n");
        sb.append("[C]").append(ALAMAT_TOKO).append("\n");
        sb.append("[C]").append(TELEPON_TOKO).append("\n");
        sb.append("[C]").append(garis).append("\n");

        sb.append("[L]No: ").append(data.kodeTransaksi).append("\n");
        sb.append("[L]Kasir: ").append(data.namaKasir).append("\n");
        sb.append("[L]Tanggal: ").append(data.waktu).append("\n");
        sb.append("[L]Shift: ")
                .append("ADMIN".equalsIgnoreCase(data.shift) ? "Admin" : "Shift " + data.shift)
                .append("\n");
        sb.append("[C]").append(garis).append("\n");

        for (StrukItem item : data.items) {
            sb.append("[L]").append(item.namaProduk).append("\n");
            sb.append("[L]  ").append(item.qty).append(" x Rp ").append(fmt.format(item.harga))
                    .append("[R]Rp ").append(fmt.format(item.subtotal)).append("\n");
        }
        sb.append("[C]").append(garis).append("\n");

        sb.append("[L]Total:[R]Rp ").append(fmt.format(data.total)).append("\n");
        sb.append("[L]Bayar:[R]Rp ").append(fmt.format(data.bayar)).append("\n");
        sb.append("[L]Kembali:[R]Rp ").append(fmt.format(data.kembalian)).append("\n");
        sb.append("[C]").append(garis).append("\n");

        sb.append("[C]Terima kasih atas kunjungannya\n");
        sb.append("[C]*** Barang yang sudah dibeli ***\n");
        sb.append("[C]*** tidak dapat ditukar ***\n");
        sb.append("\n\n");

        return sb.toString();
    }
}
