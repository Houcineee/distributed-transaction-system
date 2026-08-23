package com.houcinee.bankapp.helpers;

import java.math.BigDecimal;

public class MoneyUtil {
	private MoneyUtil() {}

	public static String formatCents(Long cents) {
		if (cents == null) return "0.00";
		return BigDecimal.valueOf(cents, 2).toPlainString();
	}
}
