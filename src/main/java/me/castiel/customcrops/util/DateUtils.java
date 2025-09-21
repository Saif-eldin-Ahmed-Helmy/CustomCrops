package me.castiel.customcrops.util;

import me.castiel.customcrops.CustomCropsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;

public class DateUtils {

    public static LocalDateTime calculateNextRotatingTime(List<String> resetTimes) {
        LocalDateTime now = LocalDateTime.now();

        List<LocalDateTime> times = resetTimes.stream()
                .map(time -> {
                    String[] splitTime = time.split(" ");
                    if (Character.isDigit(time.charAt(0)) && splitTime.length == 2) {
                        int dayOfMonth = Integer.parseInt(splitTime[0]);
                        LocalTime resetTime = LocalTime.parse(splitTime[1], DateTimeFormatter.ofPattern("HH:mm"));
                        LocalDateTime nextResetDate = LocalDateTime.of(now.withDayOfMonth(dayOfMonth).toLocalDate(), resetTime);
                        if (nextResetDate.isBefore(now)) {
                            nextResetDate = nextResetDate.plusMonths(1);
                        }
                        return nextResetDate;
                    } else if (splitTime.length == 2) {
                        DayOfWeek dayOfWeek = DayOfWeek.valueOf(splitTime[0].toUpperCase());
                        LocalTime resetTime = LocalTime.parse(splitTime[1], DateTimeFormatter.ofPattern("HH:mm"));
                        LocalDateTime nextResetDate = LocalDateTime.of(now.with(TemporalAdjusters.nextOrSame(dayOfWeek)).toLocalDate(), resetTime);
                        if (nextResetDate.isBefore(now)) {
                            nextResetDate = nextResetDate.plusWeeks(1);
                        }
                        return nextResetDate;
                    } else {
                        LocalTime resetTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
                        LocalDateTime nextResetDate = LocalDateTime.of(now.toLocalDate(), resetTime);
                        if (nextResetDate.isBefore(now)) {
                            nextResetDate = nextResetDate.plusDays(1);
                        }
                        return nextResetDate;
                    }
                })
                .toList();

        return times.stream()
                .filter(time -> time.isAfter(now))
                .min(Comparator.naturalOrder())
                .orElse(times.stream().min(Comparator.naturalOrder()).orElse(times.get(0)));
    }

    public static long getMillisecondsUntilRotation(List<String> resetTimes) {
        LocalDateTime nextRotatingTime = calculateNextRotatingTime(resetTimes);
        LocalDateTime now = LocalDateTime.now();

        Duration duration = Duration.between(now, nextRotatingTime);
        if (duration.isNegative()) {
            duration = duration.plus(1, ChronoUnit.DAYS);
        }

        return duration.toMillis();
    }

    public static String format(long seconds) {
        FileConfiguration config = CustomCropsPlugin.getInstance().getConfig();
        String weeksFormat = config.getString("TimeFormat.Weeks", "&a%weeks%w");
        String daysFormat = config.getString("TimeFormat.Days", "&a%days%d");
        String hoursFormat = config.getString("TimeFormat.Hours", "&a%hours%h");
        String minutesFormat = config.getString("TimeFormat.Minutes", "&a%minutes%m");
        String secondsFormat = config.getString("TimeFormat.Seconds", "&a%seconds%s");
        String separator = config.getString("TimeFormat.Separator", "&7 ");
        String lastSeparator = config.getString("TimeFormat.LastSeparator", "&7 and ");
        boolean showZeroValues = config.getBoolean("TimeFormat.Show-Zero-Values", false);

        long weeks = seconds / 604800;
        seconds %= 604800;
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder builder = new StringBuilder();
        if (weeks > 0 || showZeroValues) {
            builder.append(weeksFormat.replace("%weeks%", String.valueOf(weeks)));
        }
        if (days > 0 || showZeroValues) {
            if (builder.length() > 0) builder.append(separator);
            builder.append(daysFormat.replace("%days%", String.valueOf(days)));
        }
        if (hours > 0 || showZeroValues) {
            if (builder.length() > 0) builder.append(separator);
            builder.append(hoursFormat.replace("%hours%", String.valueOf(hours)));
        }
        if (minutes > 0 || showZeroValues) {
            if (builder.length() > 0) builder.append(separator);
            builder.append(minutesFormat.replace("%minutes%", String.valueOf(minutes)));
        }
        if (seconds > 0 || builder.length() == 0 || showZeroValues) {
            if (builder.length() > 0) builder.append(lastSeparator);
            builder.append(secondsFormat.replace("%seconds%", String.valueOf(seconds)));
        }

        return builder.toString();
    }

}
