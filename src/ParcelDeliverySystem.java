import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        output.println("Parcel Delivery System");
        try {
            while (true) {
                output.println("\n1. Add Parcel\n2. View All Parcels\n3. Change Delivery Service"
                        + "\n4. Mark as Delivered\n5. Exit");
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
        output.println("\n1. Document\n2. Box\n3. Fragile");
        int type = readInteger("Select parcel type: ", 1, 3, "Parcel type");
        String tracking = readTracking(true);
        String receiver = readReceiverName();
        BigDecimal maximum = new BigDecimal(type == 1 ? "0.5" : type == 2 ? "50" : "30");
        BigDecimal weight = readWeight(maximum);
        Parcel parcel;
        if (type == 1) {
            int pages = readInteger("Number of pages: ", 1, 100, "Number of pages");
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
                    parcel.getChargeableWeight().stripTrailingZeros().toPlainString(),
                    parcel.getDeliveryService().getName(), "Php " + money(parcel.getDeliveryFee()),
                    parcel.getStatus().toString()});
        }
        int[] widths = new int[7];
        for (String[] row : rows) {
            for (int column = 0; column < widths.length; column++) {
                widths[column] = Math.max(widths[column], row[column].length());
            }
        }
        StringBuilder format = new StringBuilder();
        for (int width : widths) {
            format.append("%-").append(width).append("s | ");
        }
        format.append("%n");
        output.println();
        for (int index = 0; index < rows.size(); index++) {
            output.printf(Locale.ROOT, format.toString(), (Object[]) rows.get(index));
            if (index == 0) {
                int totalWidth = 0;
                for (int width : widths) {
                    totalWidth += width + 3;
                }
                output.println("-".repeat(totalWidth));
            }
        }
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
        output.println("\n1. Standard\n2. Express\n3. Same Day");
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
                return Validation.receiverName(readLine("Receiver name (3-20 characters): "));
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
                return Validation.weight(new BigDecimal(readLine("Weight (kg): ")), maximum);
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

    private static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static final class EndOfInputException extends RuntimeException { }
}
