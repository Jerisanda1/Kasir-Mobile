package com.example.program_kasir;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.program_kasir.model.LoginResponse;

public class SessionManager {
    private static final String PREF_NAME = "KasirSession";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_NAMA  = "nama_lengkap";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_SHIFT = "shift";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(LoginResponse.UserData user, String token) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_NAMA, user.getNamaLengkap());
        editor.putString(KEY_LEVEL, user.getLevel());
        editor.putString(KEY_SHIFT, user.getShift());
        editor.apply();
    }

    public String getBearerToken() {
        return "Bearer " + prefs.getString(KEY_TOKEN, "");
    }

    public String getNamaLengkap() {
        return prefs.getString(KEY_NAMA, "");
    }

    public String getLevel() {
        return prefs.getString(KEY_LEVEL, "");
    }

    public String getShift() {
        return prefs.getString(KEY_SHIFT, null);
    }

    public boolean isLoggedIn() {
        return !prefs.getString(KEY_TOKEN, "").isEmpty();
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}