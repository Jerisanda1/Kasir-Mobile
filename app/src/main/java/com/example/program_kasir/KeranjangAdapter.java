package com.example.program_kasir;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class KeranjangAdapter extends RecyclerView.Adapter<KeranjangAdapter.KeranjangViewHolder> {

    public interface OnKeranjangActionListener {
        void onTambahQty(int position);
        void onKurangQty(int position);
        void onHapusItem(int position);
        void onManualQtyChange(int position, int newQty);
    }

    private final List<ItemKeranjang> daftarKeranjang;
    private final OnKeranjangActionListener listener;

    public KeranjangAdapter(List<ItemKeranjang> daftarKeranjang, OnKeranjangActionListener listener) {
        this.daftarKeranjang = daftarKeranjang;
        this.listener = listener;
    }

    @NonNull
    @Override
    public KeranjangViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_keranjang, parent, false);
        return new KeranjangViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KeranjangViewHolder holder, int position) {
        ItemKeranjang item = daftarKeranjang.get(position);
        NumberFormat fmt = NumberFormat.getInstance(new Locale("id", "ID"));

        holder.tvNamaItem.setText(item.getProduk().getNama());
        holder.tvHargaSatuanItem.setText(String.format("Rp %s / pcs", fmt.format(item.getProduk().getHarga())));

        // Batalkan hitungan mundur yang lama (kalau ada sisa dari baris ini sebelumnya, misal karena di-recycle)
        if (holder.debounceRunnable != null) {
            holder.debounceHandler.removeCallbacks(holder.debounceRunnable);
        }

        if (holder.qtyWatcher != null) {
            holder.tvJumlahItem.removeTextChangedListener(holder.qtyWatcher);
        }

        // Gunakan pengecekan agar tidak memicu setText jika sedang difokus,
        // untuk menghindari gangguan pada fokus dan keyboard saat recycling/re-layout.
        String val = String.valueOf(item.getJumlah());
        if (!holder.tvJumlahItem.hasFocus()) {
            holder.tvJumlahItem.setText(val);
        }

        holder.qtyWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (holder.debounceRunnable != null) {
                    holder.debounceHandler.removeCallbacks(holder.debounceRunnable);
                }

                String input = s.toString().trim();
                if (input.isEmpty()) return;

                int qty;
                try {
                    qty = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    return;
                }
                if (qty < 1) return;

                int posisiSekarang = holder.getBindingAdapterPosition();
                if (posisiSekarang == RecyclerView.NO_POSITION) return;

                ItemKeranjang itemSekarang = daftarKeranjang.get(posisiSekarang);

                // BARU: hitung batas maksimal yang boleh diisi (jumlah sekarang + sisa stok yang belum "dipegang" keranjang)
                int maksimalBoleh = itemSekarang.getJumlah() + itemSekarang.getProduk().getStok();

                // BARU: kalau melebihi batas, langsung "potong" angkanya real-time, tanpa nunggu apa pun
                if (qty > maksimalBoleh) {
                    qty = maksimalBoleh;

                    // Perbarui teks di EditText itu sendiri, tapi lepas listener dulu sementara
                    // supaya setText() ini tidak memicu afterTextChanged() lagi (infinite loop)
                    holder.tvJumlahItem.removeTextChangedListener(holder.qtyWatcher);
                    holder.tvJumlahItem.setText(String.valueOf(qty));
                    holder.tvJumlahItem.setSelection(holder.tvJumlahItem.getText().length()); // kursor ke akhir
                    holder.tvJumlahItem.addTextChangedListener(holder.qtyWatcher);

                    Toast.makeText(holder.itemView.getContext(),
                            "Stok " + itemSekarang.getProduk().getNama() + " tidak mencukupi (maks. " + maksimalBoleh + ")",
                            Toast.LENGTH_SHORT).show();
                }

                // Update subtotal baris ini secara visual langsung, pakai angka yang SUDAH divalidasi
                double subtotalSementara = itemSekarang.getProduk().getHarga() * qty;
                NumberFormat fmtLokal = NumberFormat.getInstance(new Locale("id", "ID"));
                holder.tvSubtotalItem.setText(String.format("Rp %s", fmtLokal.format(subtotalSementara)));

                final int qtyFinal = qty; // perlu final untuk dipakai di dalam Runnable
                holder.debounceRunnable = () -> {
                    int posisiTerbaru = holder.getBindingAdapterPosition();
                    if (posisiTerbaru != RecyclerView.NO_POSITION) {
                        listener.onManualQtyChange(posisiTerbaru, qtyFinal);
                    }
                };
                holder.debounceHandler.postDelayed(holder.debounceRunnable, 500);
            }
        };
        holder.tvJumlahItem.addTextChangedListener(holder.qtyWatcher);

        holder.tvJumlahItem.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus();
                return true;
            }
            return false;
        });

        holder.tvSubtotalItem.setText(String.format("Rp %s", fmt.format(item.getSubtotalItem())));

        holder.btnTambah.setOnClickListener(v -> {
            if (holder.debounceRunnable != null) {
                holder.debounceHandler.removeCallbacks(holder.debounceRunnable);
                holder.debounceRunnable = null;
            }
            holder.tvJumlahItem.clearFocus(); // BARU: paksa lepas fokus, biar update tampilan langsung jalan
            listener.onTambahQty(holder.getBindingAdapterPosition());
        });

        holder.btnKurang.setOnClickListener(v -> {
            if (holder.debounceRunnable != null) {
                holder.debounceHandler.removeCallbacks(holder.debounceRunnable);
                holder.debounceRunnable = null;
            }
            holder.tvJumlahItem.clearFocus(); // BARU
            listener.onKurangQty(holder.getBindingAdapterPosition());
        });
        holder.btnHapusItem.setOnClickListener(v -> listener.onHapusItem(holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return daftarKeranjang.size();
    }

    public static class KeranjangViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvNamaItem;
        public final TextView tvHargaSatuanItem;
        public final TextView tvSubtotalItem;
        public final EditText tvJumlahItem;
        public final ImageButton btnTambah;
        public final ImageButton btnKurang;
        public final ImageButton btnHapusItem;
        public TextWatcher qtyWatcher;
        public android.os.Handler debounceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        public Runnable debounceRunnable;

        public KeranjangViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNamaItem        = itemView.findViewById(R.id.tvNamaItem);
            tvHargaSatuanItem = itemView.findViewById(R.id.tvHargaSatuanItem);
            tvJumlahItem      = itemView.findViewById(R.id.tvJumlahItem);
            tvSubtotalItem    = itemView.findViewById(R.id.tvSubtotalItem);
            btnTambah         = itemView.findViewById(R.id.btnTambah);
            btnKurang         = itemView.findViewById(R.id.btnKurang);
            btnHapusItem      = itemView.findViewById(R.id.btnHapusItem);
        }
    }
}