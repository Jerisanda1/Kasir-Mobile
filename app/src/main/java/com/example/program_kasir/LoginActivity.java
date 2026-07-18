package com.example.program_kasir;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.program_kasir.api.ApiClient;
import com.example.program_kasir.api.ApiService;
import com.example.program_kasir.model.LoginRequest;
import com.example.program_kasir.model.LoginResponse;
import com.example.program_kasir.model.ShiftErrorResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private TextView tvShowPassword;
    private Button btnLogin;
    private boolean passwordVisible = false;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        etUsername     = findViewById(R.id.etUsername);
        etPassword     = findViewById(R.id.etPassword);
        tvShowPassword = findViewById(R.id.tvShowPassword);
        btnLogin       = findViewById(R.id.btnLogin);

        tvShowPassword.setOnClickListener(v -> togglePassword());
        btnLogin.setOnClickListener(v -> prosesLogin());
    }

    private void togglePassword() {
        if (passwordVisible) {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            tvShowPassword.setText("👁");
            passwordVisible = false;
        } else {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            tvShowPassword.setText("🙈");
            passwordVisible = true;
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void prosesLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username tidak boleh kosong!");
            etUsername.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password tidak boleh kosong!");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Memproses...");

        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.login(new LoginRequest(username, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    LoginResponse body = response.body();
                    sessionManager.saveSession(body.getUser(), body.getToken());

                    Toast.makeText(LoginActivity.this,
                            "Login berhasil! Selamat datang, " + body.getUser().getNamaLengkap(),
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    if (response.code() == 403) {
                        // Error khusus: di luar jam shift -> tampilkan card, bukan Toast
                        tampilkanDialogShiftDitolak(response);
                    } else {
                        String pesan = "Username atau password salah!";
                        if (response.errorBody() != null) {
                            try {
                                String errorJson = response.errorBody().string();
                                org.json.JSONObject obj = new org.json.JSONObject(errorJson);
                                if (obj.has("message")) {
                                    pesan = obj.getString("message");
                                }
                            } catch (Exception e) {
                                pesan = "Terjadi kesalahan pada server";
                            }
                        }
                        Toast.makeText(LoginActivity.this, pesan, Toast.LENGTH_LONG).show();
                    }
                    etPassword.setText("");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                Toast.makeText(LoginActivity.this,
                        "Gagal terhubung ke server: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void tampilkanDialogShiftDitolak(Response<LoginResponse> response) {
        try {
            String errorJson = response.errorBody().string();
            com.google.gson.Gson gson = new com.google.gson.Gson();
            ShiftErrorResponse shiftError = gson.fromJson(errorJson, ShiftErrorResponse.class);

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_shift_ditolak, null);

            TextView tvShiftAnda = dialogView.findViewById(R.id.tvShiftAnda);
            TextView tvWaktuSekarang = dialogView.findViewById(R.id.tvWaktuSekarang);
            TextView tvTanggal = dialogView.findViewById(R.id.tvTanggal);
            TextView btnMengerti = dialogView.findViewById(R.id.btnMengerti);

            if (shiftError.getData() != null) {
                tvShiftAnda.setText("Shift Anda: " + shiftError.getData().getShiftInfo());
                tvWaktuSekarang.setText("Waktu Sekarang: " + shiftError.getData().getWaktuSekarang());
                tvTanggal.setText("Tanggal: " + shiftError.getData().getTanggal());
            }

            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            // Hilangkan background putih bawaan dialog, biar cuma kartu custom yang terlihat
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            btnMengerti.setOnClickListener(v -> dialog.dismiss());

            dialog.show();

        } catch (Exception e) {
            Toast.makeText(LoginActivity.this, "Di luar jam shift Anda", Toast.LENGTH_LONG).show();
        }
    }
}