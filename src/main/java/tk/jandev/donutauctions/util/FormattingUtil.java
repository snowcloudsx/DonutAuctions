package tk.jandev.donutauctions.util;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class FormattingUtil {
    private final static int[] exponents = new int[]{3, 6, 9, 12};
    private final static String[] signs = new String[]{"k", "M", "B", "T"};
    private final static DecimalFormat TWO_DECIMALS = new DecimalFormat("#.##");


    public static String formatCurrency(long money) {
        for (int i = exponents.length - 1; i >= 0; i--) {
            double divisor = Math.pow(10, exponents[i]);

            if ((money / divisor) >= 1) {
                double formatted = money / divisor;

                String formattedValue = TWO_DECIMALS.format(formatted);
                if (formattedValue.equals("1000") && (i+1) < exponents.length) { // prevent rounding 999.9999k to 1000k -> make it 1m
                    return "1" + signs[i + 1];
                }
                return TWO_DECIMALS.format(formatted) + signs[i];
            }
        }

        return String.valueOf(money);
    }
}
