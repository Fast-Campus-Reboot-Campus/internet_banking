package com.bank.loan.commonsync.dto;

public record CommonSyncDispatchSummary(int total, int done, int failed, int dead) {

    public static CommonSyncDispatchSummary of(int total, int done, int failed, int dead) {
        return new CommonSyncDispatchSummary(total, done, failed, dead);
    }
}
