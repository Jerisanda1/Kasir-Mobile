package com.example.program_kasir;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
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
        void onUbahQtyManual(int position, int newQty);
    }

    private List<ItemKeranjang> daftarKeranjang;
    private OnKeranjangActionListener listener;

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
        holder.tvHargaSatuanItem.setText("Rp " + fmt.format(item.getProduk().getHarga()) + " / pcs");
        holder.tvSubtotalItem.setText("Rp " + fmt.format(item.getSubtotalItem()));

        // Atur teks Qty tanpa memicu TextWatcher (pakai flag atau hapus sementara)
        holder.isBinding = true;
        holder.etJumlahItem.setText(String.valueOf(item.getJumlah()));
        holder.isBinding = false;

        holder.btnTambah.setOnClickListener(v -> {
            holder.etJumlahItem.clearFocus();
            listener.onTambahQty(holder.getAdapterPosition());
        });
        holder.btnKurang.setOnClickListener(v -> {
            holder.etJumlahItem.clearFocus();
            listener.onKurangQty(holder.getAdapterPosition());
        });
        holder.btnHapusItem.setOnClickListener(v -> {
            holder.etJumlahItem.clearFocus();
            listener.onHapusItem(holder.getAdapterPosition());
        });

        // Handle enter key atau tombol "Done" pada keyboard
        holder.etJumlahItem.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || 
               (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                
                holder.etJumlahItem.clearFocus();
                return true;
            }
            return false;
        });

        // Simpan otomatis saat fokus hilang
        holder.etJumlahItem.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                prosesUpdateQty(holder);
            }
        });
    }

    private void prosesUpdateQty(KeranjangViewHolder holder) {
        int pos = holder.getAdapterPosition();
        if (pos == RecyclerView.NO_POSITION) return;

        ItemKeranjang currentItem = daftarKeranjang.get(pos);
        String input = holder.etJumlahItem.getText().toString().trim();
        
        if (input.isEmpty()) {
            holder.etJumlahItem.setText(String.valueOf(currentItem.getJumlah()));
            return;
        }

        try {
            int newQty = Integer.parseInt(input);
            // HANYA update jika angka baru berbeda dengan angka lama
            if (newQty != currentItem.getJumlah()) {
                if (newQty > 0) {
                    listener.onUbahQtyManual(pos, newQty);
                } else {
                    listener.onHapusItem(pos);
                }
            }
        } catch (NumberFormatException e) {
            holder.etJumlahItem.setText(String.valueOf(currentItem.getJumlah()));
        }
    }

    @Override
    public int getItemCount() {
        return daftarKeranjang.size();
    }

    static class KeranjangViewHolder extends RecyclerView.ViewHolder {
        TextView tvNamaItem, tvHargaSatuanItem, tvSubtotalItem;
        EditText etJumlahItem;
        ImageButton btnTambah, btnKurang, btnHapusItem;
        boolean isBinding = false;

        public KeranjangViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNamaItem        = itemView.findViewById(R.id.tvNamaItem);
            tvHargaSatuanItem = itemView.findViewById(R.id.tvHargaSatuanItem);
            etJumlahItem      = itemView.findViewById(R.id.etJumlahItem);
            tvSubtotalItem    = itemView.findViewById(R.id.tvSubtotalItem);
            btnTambah         = itemView.findViewById(R.id.btnTambah);
            btnKurang         = itemView.findViewById(R.id.btnKurang);
            btnHapusItem      = itemView.findViewById(R.id.btnHapusItem);
        }
    }
}
