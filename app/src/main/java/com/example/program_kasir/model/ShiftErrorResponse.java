package com.example.program_kasir.model;

public class ShiftErrorResponse {
    private boolean success;
    private String error_type;
    private String message;
    private ShiftErrorData data;

    public boolean isSuccess() { return success; }
    public String getErrorType() { return error_type; }
    public String getMessage() { return message; }
    public ShiftErrorData getData() { return data; }

    public static class ShiftErrorData {
        private String shift_info;
        private String waktu_sekarang;
        private String tanggal;

        public String getShiftInfo() { return shift_info; }
        public String getWaktuSekarang() { return waktu_sekarang; }
        public String getTanggal() { return tanggal; }
    }
}