package net.okocraft.yaminabe.common.restart;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RestartDurationParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "^(?:(\\d+)|(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?)$",
        Pattern.CASE_INSENSITIVE
    );

    public static Duration parse(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("duration cannot be empty");
        }

        Matcher matcher = DURATION_PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw invalid(input, null);
        }

        try {
            if (matcher.group(1) != null) {
                return Duration.ofSeconds(Long.parseLong(matcher.group(1)));
            }
            if (matcher.group(2) == null && matcher.group(3) == null
                && matcher.group(4) == null && matcher.group(5) == null) {
                throw invalid(input, null);
            }

            Duration result = Duration.ZERO;
            for (int group = 2; group <= 5; group++) {
                if (matcher.group(group) == null) {
                    continue;
                }
                long amount = Long.parseLong(matcher.group(group));
                result = switch (group) {
                    case 2 -> result.plusDays(amount);
                    case 3 -> result.plusHours(amount);
                    case 4 -> result.plusMinutes(amount);
                    case 5 -> result.plusSeconds(amount);
                    default -> throw new AssertionError();
                };
            }
            return result;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw invalid(input, exception);
        }
    }

    private static IllegalArgumentException invalid(String input, Exception cause) {
        return new IllegalArgumentException("invalid duration: " + input, cause);
    }

    private RestartDurationParser() {
        throw new UnsupportedOperationException();
    }
}
