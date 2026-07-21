package com.example.program_kasir;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProdukAdapter extends RecyclerView.Adapter<ProdukAdapter.ProdukViewHolder> {

    public interface OnProdukClickListener {
        void onProdukClick(Produk produk);
    }

    private List<Produk> daftarProduk;
    private OnProdukClickListener listener;

    public ProdukAdapter(List<Produk> daftarProduk, OnProdukClickListener listener) {
        this.daftarProduk = daftarProduk;
        this.listener = listener;
    }

    public void updateData(List<Produk> dataBaru) {
        this.daftarProduk = dataBaru;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProdukViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_produk, parent, false);
        return new ProdukViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProdukViewHolder holder, int position) {
        Produk produk = daftarProduk.get(position);
        NumberFormat fmt = NumberFormat.getInstance(new Locale("id", "ID"));

        holder.tvNamaProduk.setText(produk.getNama());
        holder.tvHargaProduk.setText("Rp " + fmt.format(produk.getHarga()));

        if (produk.getFotoUrl() != null && !produk.getFotoUrl().isEmpty()) {
            holder.tvIconProduk.setVisibility(View.GONE);
            holder.ivFotoProduk.setVisibility(View.VISIBLE);

            Glide.with(holder.itemView.getContext())
                    .load(produk.getFotoUrl())
                    .transform(new RoundedCorners(24))
                    .into(holder.ivFotoProduk);
        } else {
            holder.tvIconProduk.setVisibility(View.VISIBLE);
            holder.ivFotoProduk.setVisibility(View.GONE);
            holder.tvIconProduk.setText(produk.getIconEmoji());
        }

        holder.tvStokProduk.setText(produk.getStok() + " Stok");

// Cek status produk: kadaluarsa atau stok habis (blokir klik)
        if (produk.isTidakBisaDijual()) {
            holder.tvBadgeStatus.setVisibility(View.VISIBLE);
            holder.tvBadgeStatus.setText(produk.getLabelTidakBisaDijual());
            holder.tvBadgeWarning.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
        } else {
            holder.tvBadgeStatus.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> listener.onProdukClick(produk));

            // Peringatan (tetap bisa diklik): stok menipis / segera expired
            if (produk.adaPeringatan()) {
                holder.tvBadgeWarning.setVisibility(View.VISIBLE);
                holder.tvBadgeWarning.setText(produk.getLabelPeringatan());
            } else {
                holder.tvBadgeWarning.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return daftarProduk.size();
    }

    static class ProdukViewHolder extends RecyclerView.ViewHolder {
        TextView tvIconProduk, tvNamaProduk, tvHargaProduk, tvBadgeStatus, tvStokProduk, tvBadgeWarning;
        ImageView ivFotoProduk;
        View cardProduk;

        public ProdukViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIconProduk   = itemView.findViewById(R.id.tvIconProduk);
            tvNamaProduk   = itemView.findViewById(R.id.tvNamaProduk);
            tvHargaProduk  = itemView.findViewById(R.id.tvHargaProduk);
            ivFotoProduk   = itemView.findViewById(R.id.ivFotoProduk);
            tvBadgeStatus  = itemView.findViewById(R.id.tvBadgeStatus);
            tvStokProduk   = itemView.findViewById(R.id.tvStokProduk);
            tvBadgeWarning = itemView.findViewById(R.id.tvBadgeWarning);
            cardProduk     = itemView.findViewById(R.id.cardProduk);
        }
    }
}