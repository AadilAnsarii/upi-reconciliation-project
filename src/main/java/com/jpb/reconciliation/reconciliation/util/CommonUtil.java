package com.jpb.reconciliation.reconciliation.util;

public class CommonUtil {
	public static String removeAllWhitespace(String input) {
		if (input == null) {
			return null;
		}
		return input.replaceAll("\\s+", "");
	}
}
