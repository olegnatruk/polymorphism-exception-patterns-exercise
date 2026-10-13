import java.math.BigDecimal;

final class Validation {
    private Validation() { }

    static String text(String value, String field) throws InvalidParcelException {
        if (value == null || value.isBlank()) {
            throw new InvalidParcelException(field + " must not be empty!");
        }
        return value.strip();
    }

    static String receiverName(String value) throws InvalidParcelException {
        String name = text(value, "Receiver name");
        int length = name.codePointCount(0, name.length());
        if (length < 3 || length > 20) {
            throw new InvalidParcelException("Receiver name must be 3-20 characters!");
        }
        return name;
    }

    static String tracking(String value) throws InvalidParcelException {
        String tracking = text(value, "Tracking number");
        if (!tracking.matches("ABC[0-9]{4}")) {
            throw new InvalidParcelException("Tracking number must be ABC followed by 4 digits!");
        }
        return tracking;
    }

    static BigDecimal weight(BigDecimal value, BigDecimal maximum)
            throws InvalidParcelException {
        if (value == null || value.signum() <= 0 || value.compareTo(maximum) > 0) {
            throw new InvalidParcelException("Weight must be greater than 0 and at most "
                    + maximum.toPlainString() + " kg!");
        }
        return value;
    }

    static int integer(int value, int minimum, int maximum, String field)
            throws InvalidParcelException {
        if (value < minimum || value > maximum) {
            throw new InvalidParcelException(field + (maximum == Integer.MAX_VALUE
                    ? " must be a positive integer!"
                    : " must be between " + minimum + " and " + maximum + "!"));
        }
        return value;
    }
}
