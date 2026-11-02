package pccafe.util;

public class TimeFormat {
    private TimeFormat() {
    }

    // 3725 -> "01:02:05"
    public static String format(int seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60);
    }
}
