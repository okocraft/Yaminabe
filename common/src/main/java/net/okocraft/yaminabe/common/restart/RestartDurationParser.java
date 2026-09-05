package net.okocraft.yaminabe.common.restart;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RestartDurationParser {

    private static final Pattern PART_PATTERN = Pattern.compile("(\\d+)([dhms])", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECONDS_PATTERN = Pattern.compile("\\d+");

    public static Duration parse(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("duration cannot be empty");
        }

        if (SECONDS_PATTERN.matcher(input).matches()) {
            return Duration.ofSeconds(parseNumber(input));
        }

        Matcher matcher = PART_PATTERN.matcher(input);
        Duration result = Duration.ZERO;
        int end = 0;
        boolean found = false;
        try {
            while (matcher.find()) {
                if (matcher.start() != end) {
                    throw invalid(input);
                }
                found = true;
                long amount = parseNumber(matcher.group(1));
                result = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
                    case "d" -> result.plusDays(amount);
                    case "h" -> result.plusHours(amount);
                    case "m" -> result.plusMinutes(amount);
                    case "s" -> result.plusSeconds(amount);
                    default -> throw invalid(input);
                };
                end = matcher.end();
            }
        } catch (ArithmeticException exception) {
            throw invalid(input, exception);
        }

        if (!found || end != input.length()) {
            throw invalid(input);
        }
        return result;
    }

    private static long parseNumber(String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException exception) {
            throw invalid(input, exception);
        }
    }

    private static IllegalArgumentException invalid(String input) {
        return new IllegalArgumentException("invalid duration: " + input);
    }

    private static IllegalArgumentException invalid(String input, Exception cause) {
        return new IllegalArgumentException("invalid duration: " + input, cause);
    }

    private RestartDurationParser() {
        throw new UnsupportedOperationException();
    }
}
