package com.example.program_kasir;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.program_kasir.model.TransaksiRiwayat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RiwayatAdapter extends RecyclerView.Adapter<RiwayatAdapter.RiwayatViewHolder> {

    public interface OnRiwayatActionListener {
        void onDetailClick(TransaksiRiwayat item);
        void onCetakClick(TransaksiRiwayat item);
    }

    private List<TransaksiRiwayat> daftarRiwayat;
    private final OnRiwayatActionListener listener;

    public RiwayatAdapter(List<TransaksiRiwayat> daftarRiwayat, OnRiwayatActionListener listener) {
        this.daftarRiwayat = daftarRiwayat;
        this.listener = listener;
    }

    public void updateData(List<TransaksiRiwayat> dataBaru) {
        this.daftarRiwayat = dataBaru;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RiwayatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_riwayat, parent, false);
        return new RiwayatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RiwayatViewHolder holder, int position) {
        TransaksiRiwayat item = daftarRiwayat.get(position);
        NumberFormat fmt = NumberFormat.getInstance(new Locale("id", "ID"));

        holder.tvKodeTransaksi.setText(item.getKodeTransaksi());
        holder.tvTotal.setText("Rp " + fmt.format(item.getTotal()));
        holder.tvShift.setText("ADMIN".equalsIgnoreCase(item.getShift()) ? "Admin" : "Shift " + item.getShift());
        holder.tvBayarKembalian.setText("Bayar Rp " + fmt.format(item.getBayar())
                + "  •  Kembali Rp " + fmt.format(item.getKembalian()));
        holder.tvWaktu.setText(formatWaktu(item.getCreatedAt()));

        holder.btnDetail.setOnClickListener(v -> listener.onDetailClick(item));
        holder.btnCetak.setOnClickListener(v -> listener.onCetakClick(item));
    }

    private String formatWaktu(String createdAt) {
        if (createdAt == null) return "-";
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("dd MMM yyyy • HH:mm", new Locale("id", "ID"));
            return output.format(input.parse(createdAt));
        } catch (Exception e) {
            return createdAt; // fallback kalau format tanggal dari server beda
        }
    }

    @Override
    public int getItemCount() {
        return daftarRiwayat.size();
    }

    static class RiwayatViewHolder extends RecyclerView.ViewHolder {
        TextView tvKodeTransaksi, tvTotal, tvWaktu, tvShift, tvBayarKembalian;
        android.widget.Button btnDetail, btnCetak;

        public RiwayatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKodeTransaksi   = itemView.findViewById(R.id.tvKodeTransaksi);
            tvTotal           = itemView.findViewById(R.id.tvTotal);
            tvWaktu           = itemView.findViewById(R.id.tvWaktu);
            tvShift           = itemView.findViewById(R.id.tvShift);
            tvBayarKembalian  = itemView.findViewById(R.id.tvBayarKembalian);
            btnDetail         = itemView.findViewById(R.id.btnDetail);
            btnCetak          = itemView.findViewById(R.id.btnCetak);
        }
    }
}
