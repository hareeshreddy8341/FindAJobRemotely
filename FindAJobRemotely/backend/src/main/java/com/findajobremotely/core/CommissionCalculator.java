package com.findajobremotely.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Commission owed by a candidate once hired: a percentage of the agreed first-month pay. */
public final class CommissionCalculator {

    private CommissionCalculator() {
    }

    /** Whole currency units, rounded half-up. */
    public static long amount(long agreedMonthlyPay, BigDecimal percent) {
        if (agreedMonthlyPay <= 0) {
            throw new IllegalArgumentException("Agreed monthly pay must be positive");
        }
        if (percent == null || percent.signum() < 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Commission percent must be between 0 and 100");
        }
        return BigDecimal.valueOf(agreedMonthlyPay)
                .multiply(percent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
