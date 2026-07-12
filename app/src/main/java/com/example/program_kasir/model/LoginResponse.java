package com.example.program_kasir.model;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private UserData user;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
    public UserData getUser() { return user; }

    public static class UserData {
        private String kode_login;
        private String username;
        private String nama_lengkap;
        private String level;
        private String shift;

        public String getKodeLogin() { return kode_login; }
        public String getUsername() { return username; }
        public String getNamaLengkap() { return nama_lengkap; }
        public String getLevel() { return level; }
        public String getShift() { return shift; }
    }
}