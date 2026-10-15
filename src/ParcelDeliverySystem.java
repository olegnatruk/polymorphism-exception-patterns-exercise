import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class ParcelDeliverySystem {
    private final Scanner input;
    private final PrintStream output;
    private final ParcelManager manager = ParcelManager.getInstance();

    public ParcelDeliverySystem(InputStream input, PrintStream output) {
        this.input = new Scanner(input);
        this.output = output;
    }

    public static void main(String[] args) {
        new ParcelDeliverySystem(System.in, System.out).run();
    }

    public void run() {
        try {
            while (true) {
                printBox("Parcel Delivery System", "1. Add Parcel", "2. View All Parcels",
                        "3. Change Delivery Service", "4. Mark as Delivered", "5. Exit");
                int choice = readInteger("Choose an option: ", 1, 5, "Menu option");
                try {
                    switch (choice) {
                        case 1 -> addParcel();
                        case 2 -> viewAllParcels();
                        case 3 -> changeDeliveryService();
                        case 4 -> markAsDelivered();
                        case 5 -> {
                            output.println("Goodbye!");
                            return;
                        }
                        default -> throw new IllegalStateException("Unexpected menu option");
                    }
                } catch (ParcelException exception) {
                    output.println(exception.getMessage());
                }
            }
        } catch (EndOfInputException exception) {
            output.println("\nInput ended. Goodbye!");
        }
    }

    private void addParcel() throws ParcelException {
        printBox("Parcel Type", "1. Document", "2. Box", "3. Fragile");
        int type = readInteger("Select parcel type: ", 1, 3, "Parcel type");
        String tracking = readTracking(true);
        String receiver = readReceiverName();
        BigDecimal maximum = new BigDecimal(type == 1 ? "0.5" : type == 2 ? "50" : "30");
        BigDecimal weight = readWeight(maximum);
        Parcel parcel;
        if (type == 1) {
            int pages = readInteger("Number of pages (minimum: 1, maximum: 100): ",
                    1, 100, "Number of pages");
            parcel = new Document(tracking, receiver, weight, pages);
        } else {
            int length = readInteger("Length (cm): ", 1, Integer.MAX_VALUE, "Length");
            int width = readInteger("Width (cm): ", 1, Integer.MAX_VALUE, "Width");
            int height = readInteger("Height (cm): ", 1, Integer.MAX_VALUE, "Height");
            parcel = type == 2 ? new Box(tracking, receiver, weight, length, width, height)
                    : new Fragile(tracking, receiver, weight, length, width, height,
                            readText("Handling note: ", "Handling note"));
        }
        while (true) {
            try {
                parcel.changeDeliveryService(readDeliveryService());
                break;
            } catch (DeliveryServiceUnavailableException exception) {
                output.println(exception.getMessage());
            }
        }
        manager.addParcel(parcel);
        output.println("Parcel added successfully! Delivery fee: Php " + money(parcel.getDeliveryFee()));
    }

    private void viewAllParcels() {
        List<Parcel> parcels = manager.getAllParcels();
        if (parcels.isEmpty()) {
            output.println("No parcels available.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Tracking Number", "Type", "Receiver", "Chargeable Weight (kg)",
                "Service", "Delivery Fee", "Status"});
        // Polymorphism: each Parcel supplies its own type, weight, and fee behavior.
        for (Parcel parcel : parcels) {
            rows.add(new String[] {parcel.getTrackingNumber(), parcel.getType(), parcel.getReceiverName(),
                    displayWeight(parcel.getChargeableWeight()),
                    parcel.getDeliveryService().getName(), "Php " + money(parcel.getDeliveryFee()),
                    parcel.getStatus().toString()});
        }
        int[] widths = new int[7];
        for (String[] row : rows) {
            for (int column = 0; column < widths.length; column++) {
                widths[column] = Math.max(widths[column], textWidth(row[column]));
            }
        }
        output.println();
        output.println(tableBorder(widths, '┌', '┬', '┐'));
        for (int index = 0; index < rows.size(); index++) {
            StringBuilder line = new StringBuilder("│");
            String[] row = rows.get(index);
            for (int column = 0; column < widths.length; column++) {
                line.append(' ').append(padRight(row[column], widths[column])).append(" │");
            }
            output.println(line);
            if (index == 0) {
                output.println(tableBorder(widths, '├', '┼', '┤'));
            }
        }
        output.println(tableBorder(widths, '└', '┴', '┘'));
    }

    private void changeDeliveryService() throws ParcelException {
        if (manager.getAllParcels().isEmpty()) {
            output.println("No parcels available.");
            return;
        }
        String tracking = readTracking(false);
        Parcel parcel = manager.findParcel(tracking);
        parcel.ensurePending();
        DeliveryStrategy service = readDeliveryService();
        if (parcel.getDeliveryService().getName().equals(service.getName())) {
            output.println("It is already " + service.getName());
            return;
        }
        manager.changeDeliveryService(tracking, service);
        output.println("Delivery service changed! New fee: Php "
                + money(parcel.getDeliveryFee()));
    }

    private void markAsDelivered() throws ParcelException {
        if (manager.getAllParcels().isEmpty()) {
            output.println("No parcels available.");
            return;
        }
        boolean delivered = manager.markAsDelivered(readTracking(false));
        output.println(delivered ? "Parcel delivered successfully!" : "Parcel was already delivered.");
    }

    private DeliveryStrategy readDeliveryService() {
        printBox("Delivery Service", "1. Standard", "2. Express", "3. Same Day");
        return switch (readInteger("Select delivery service: ", 1, 3, "Delivery service")) {
            case 1 -> new StandardDelivery();
            case 2 -> new ExpressDelivery();
            case 3 -> new SameDayDelivery();
            default -> throw new IllegalStateException("Unexpected delivery service");
        };
    }

    private String readTracking(boolean mustBeUnique) {
        while (true) {
            try {
                String tracking = Validation.tracking(readLine("Tracking number (ABC0000): "));
                if (mustBeUnique) {
                    manager.checkTrackingAvailable(tracking);
                }
                return tracking;
            } catch (ParcelException exception) {
                output.println(exception.getMessage());
            }
        }
    }

    private String readReceiverName() {
        while (true) {
            try {
                return Validation.receiverName(readLine("Receiver name (3-20 characters, no numbers): "));
            } catch (InvalidParcelException exception) {
                output.println(exception.getMessage());
            }
        }
    }

    private String readText(String prompt, String field) {
        while (true) {
            try {
                return Validation.text(readLine(prompt), field);
            } catch (InvalidParcelException exception) {
                output.println(exception.getMessage());
            }
        }
    }

    private int readInteger(String prompt, int minimum, int maximum, String field) {
        while (true) {
            try {
                return Validation.integer(Integer.parseInt(readLine(prompt)), minimum, maximum, field);
            } catch (NumberFormatException exception) {
                output.println(field + " must be a valid whole number!");
            } catch (InvalidParcelException exception) {
                output.println(exception.getMessage());
            }
        }
    }

    private BigDecimal readWeight(BigDecimal maximum) {
        while (true) {
            try {
                String prompt = "Weight (kg, minimum: > 0, maximum: " + maximum.toPlainString() + "): ";
                return Validation.weight(new BigDecimal(readLine(prompt)), maximum);
            } catch (NumberFormatException exception) {
                output.println("Weight must be a valid decimal number!");
            } catch (InvalidParcelException exception) {
                output.println(exception.getMessage());
            }
        }
    }

    private String readLine(String prompt) {
        output.print(prompt);
        output.flush();
        if (!input.hasNextLine()) {
            throw new EndOfInputException();
        }
        return input.nextLine().strip();
    }

    private void printBox(String title, String... lines) {
        int width = textWidth(title);
        for (String line : lines) {
            width = Math.max(width, textWidth(line));
        }
        String border = "═".repeat(width + 2);
        output.println();
        output.println("╔" + border + "╗");
        output.println("║ " + padRight(title, width) + " ║");
        output.println("╠" + border + "╣");
        for (String line : lines) {
            output.println("║ " + padRight(line, width) + " ║");
        }
        output.println("╚" + border + "╝");
    }

    private static String tableBorder(int[] widths, char left, char middle, char right) {
        StringBuilder border = new StringBuilder().append(left);
        for (int column = 0; column < widths.length; column++) {
            if (column > 0) {
                border.append(middle);
            }
            border.append("─".repeat(widths[column] + 2));
        }
        return border.append(right).toString();
    }

    private static int textWidth(String text) {
        return text.codePointCount(0, text.length());
    }

    private static String padRight(String text, int width) {
        return text + " ".repeat(width - textWidth(text));
    }

    private static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String displayWeight(BigDecimal weight) {
        BigDecimal normalized = weight.stripTrailingZeros();
        // Avoid expanding extreme exponents into millions or billions of zeroes.
        return normalized.scale() > 20 ? normalized.toString() : normalized.toPlainString();
    }

    private static final class EndOfInputException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
