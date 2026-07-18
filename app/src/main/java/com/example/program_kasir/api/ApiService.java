package com.example.program_kasir.api;

import com.example.program_kasir.model.DetailRiwayatResponse;
import com.example.program_kasir.model.LoginRequest;
import com.example.program_kasir.model.LoginResponse;
import com.example.program_kasir.model.ProdukResponse;
import com.example.program_kasir.model.RiwayatResponse;
import com.example.program_kasir.model.TransaksiRequest;
import com.example.program_kasir.model.TransaksiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("produk")
    Call<ProdukResponse> getProduk(@Header("Authorization") String token,
                                   @Query("keyword") String keyword);

    @POST("transaksi")
    Call<TransaksiResponse> checkout(@Header("Authorization") String token,
                                     @Body TransaksiRequest request);

    @GET("riwayat-transaksi")
    Call<RiwayatResponse> getRiwayat(@Header("Authorization") String token,
                                      @Query("search") String search,
                                      @Query("date") String date,
                                      @Query("bulan") String bulan);

    @GET("riwayat-transaksi/detail/{kode}")
    Call<DetailRiwayatResponse> getDetailRiwayat(@Header("Authorization") String token,
                                                  @Path("kode") String kodeTransaksi);
}