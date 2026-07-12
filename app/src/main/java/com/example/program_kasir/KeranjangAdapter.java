package com.example.program_kasir;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class KeranjangAdapter extends RecyclerView.Adapter<KeranjangAdapter.KeranjangViewHolder> {

    // Interface untuk komunikasi aksi (tambah/kurang qty/hapus) ke Activity
    public interface OnKeranjangActionListener {
        void onTambahQty(int position);
        void onKurangQty(int position);
        void onHapusItem(int position);
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
        holder.tvJumlahItem.setText(String.valueOf(item.getJumlah()));
        holder.tvSubtotalItem.setText("Rp " + fmt.format(item.getSubtotalItem()));

        holder.btnTambah.setOnClickListener(v -> listener.onTambahQty(holder.getAdapterPosition()));
        holder.btnKurang.setOnClickListener(v -> listener.onKurangQty(holder.getAdapterPosition()));
        holder.btnHapusItem.setOnClickListener(v -> listener.onHapusItem(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return daftarKeranjang.size();
    }

    static class KeranjangViewHolder extends RecyclerView.ViewHolder {
        TextView tvNamaItem, tvHargaSatuanItem, tvJumlahItem, tvSubtotalItem;
        TextView btnTambah, btnKurang, btnHapusItem;

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