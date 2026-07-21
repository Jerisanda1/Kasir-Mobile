package com.example.program_kasir.model;

public class ShiftKasResponse {
    private boolean success;
    private String message;
    private ShiftKasSesi data; // null kalau belum ada shift aktif

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public ShiftKasSesi getData() { return data; }
}