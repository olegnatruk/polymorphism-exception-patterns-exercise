import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Dependency-free regression tests: failures throw AssertionError and exit nonzero. */
public final class ParcelDeliverySystemTest {
    private static int checks;
    private static int nextTracking = 1;

    public static void main(String[] args) throws Exception {
        contains(console("2\n5\n"), "No parcels available.");
        for (int choice : new int[] {3, 4}) {
            String transcript = console(choice + "\n5\n");
            contains(transcript, "No parcels available.");
            contains(transcript, "Goodbye!");
            check(!transcript.contains("Tracking number (ABC0000): "),
                    "Empty collection must not prompt for a tracking number");
            check(!transcript.contains("Input ended."),
                    "Empty collection must return to the menu and accept Exit");
        }
        testDocuments();
        testBoxesAndFragile();
        testStrategies();
        testValidation();
        testManager();
        testConsole();
        testUnchangedDeliveryService();
        testEndOfInput();
        System.out.println("All " + checks + " checks passed.");
    }

    private static void testDocuments() throws Exception {
        Document first = document("0.001", 1);
        Document last = document("0.5", 100);
        equal(first.getNumberOfPages(), 1);
        equal(last.getNumberOfPages(), 100);
        decimal(first.getChargeableWeight(), "0.001");
        decimal(last.getChargeableWeight(), "0.5");
        decimal(last.getBaseFee(), "50");
        fails(InvalidParcelException.class, () -> document("0.50001", 1));
        fails(InvalidParcelException.class, () -> document("0.1", 0));
        fails(InvalidParcelException.class, () -> document("0.1", 101));
        fails(InvalidParcelException.class, () -> document("0", 10));
        fails(InvalidParcelException.class, () -> document("-0.1", 10));
    }

    private static void testBoxesAndFragile() throws Exception {
        decimal(box("2.0", 1, 1, 1).getChargeableWeight(), "2");
        decimal(box("2.1", 1, 1, 1).getChargeableWeight(), "3");
        decimal(box("1", 1, 1, 7000).getChargeableWeight(), "2");
        decimal(box("1", 1, 1, 7001).getChargeableWeight(), "3");
        decimal(box("0.01", 1, 1, 1).getChargeableWeight(), "1");
        decimal(box("50", 1, 1, 1).getBaseFee(), "2000");
        fails(InvalidParcelException.class, () -> box("50.0001", 1, 1, 1));
        fails(InvalidParcelException.class, () -> box("0", 1, 1, 1));
        fails(InvalidParcelException.class, () -> box("-1", 1, 1, 1));
        fails(InvalidParcelException.class, () -> box("1", 0, 1, 1));
        fails(InvalidParcelException.class, () -> box("1", 1, -1, 1));
        fails(InvalidParcelException.class, () -> box("1", 1, 1, 0));
        Box huge = box("1", Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        String expected = BigInteger.valueOf(Integer.MAX_VALUE).pow(3)
                .add(BigInteger.valueOf(3499)).divide(BigInteger.valueOf(3500)).toString();
        decimal(huge.getChargeableWeight(), expected);
        Fragile fragile = fragile("1.8", "  Keep upright  ");
        check(fragile instanceof Box, "Fragile must extend Box");
        equal(fragile.getHandlingNote(), "Keep upright");
        decimal(fragile.getChargeableWeight(), "3");
        decimal(fragile.getBaseFee(), "220");
        fragile.changeDeliveryService(new ExpressDelivery());
        decimal(fragile.getDeliveryFee(), "330.00");
        decimal(fragile("30", "Careful").getBaseFee(), "1300");
        fails(InvalidParcelException.class, () -> fragile("30.0001", "Careful"));
        fails(InvalidParcelException.class, () -> fragile("0", "Careful"));
        fails(InvalidParcelException.class, () -> fragile("1", "  "));
        fails(InvalidParcelException.class, () -> fragile("1", null));
        fails(InvalidParcelException.class, () -> new Fragile(tracking(), "Ana", bd("1"),
                -1, 20, 10, "Careful"));

        List<Parcel> parcels = List.of(document("0.2", 5), box("2.1", 1, 1, 1), fragile);
        String[] expectedFees = {"50", "120", "220"};
        String[] expectedTypes = {"Document", "Box", "Fragile"};
        for (int index = 0; index < parcels.size(); index++) {
            decimal(parcels.get(index).getBaseFee(), expectedFees[index]);
            equal(parcels.get(index).getType(), expectedTypes[index]);
        }
    }

    private static void testStrategies() throws Exception {
        Parcel[] parcels = {document("0.5", 100), box("2.1", 1, 1, 1), fragile("1.8", "Careful")};
        String[][] fees = {{"50", "75", "200"}, {"120", "180", "340"}, {"220", "330", "540"}};
        DeliveryStrategy[] services = {new StandardDelivery(), new ExpressDelivery(), new SameDayDelivery()};
        String[] names = {"Standard", "Express", "Same Day"};
        for (int parcelIndex = 0; parcelIndex < parcels.length; parcelIndex++) {
            for (int serviceIndex = 0; serviceIndex < services.length; serviceIndex++) {
                parcels[parcelIndex].changeDeliveryService(services[serviceIndex]);
                decimal(parcels[parcelIndex].getDeliveryFee(), fees[parcelIndex][serviceIndex]);
                equal(parcels[parcelIndex].getDeliveryService().getName(), names[serviceIndex]);
            }
        }
        Box ten = box("10", 1, 1, 1);
        ten.changeDeliveryService(new SameDayDelivery());
        decimal(ten.getDeliveryFee(), "900");
        Box volumetricTen = box("1", 35, 100, 10);
        volumetricTen.changeDeliveryService(new SameDayDelivery());
        decimal(volumetricTen.getDeliveryFee(), "900");
        Box aboveTen = box("10.0001", 1, 1, 1);
        aboveTen.changeDeliveryService(new ExpressDelivery());
        DeliveryStrategy oldService = aboveTen.getDeliveryService();
        fails(DeliveryServiceUnavailableException.class,
                () -> aboveTen.changeDeliveryService(new SameDayDelivery()));
        check(aboveTen.getDeliveryService() == oldService, "Rejected service must preserve strategy");
        decimal(aboveTen.getDeliveryFee(), "660");
        Box volumetricEleven = box("1", 1, 1, 35001);
        fails(DeliveryServiceUnavailableException.class,
                () -> volumetricEleven.changeDeliveryService(new SameDayDelivery()));
        equal(volumetricEleven.getDeliveryService().getName(), "Standard");
        fails(InvalidParcelException.class, () -> ten.changeDeliveryService(null));
    }

    private static void testValidation() throws Exception {
        for (String invalid : new String[] {"abc0001", "ABC123", "ABC12345", "XYZ0001", "ABC12a4", "", "ABC１２３４"}) {
            fails(InvalidParcelException.class,
                    () -> new Document(invalid, "Ana", bd("0.1"), 1));
        }
        fails(InvalidParcelException.class, () -> new Document(null, "Ana", bd("0.1"), 1));
        fails(InvalidParcelException.class, () -> new Document(tracking(), "  ", bd("0.1"), 1));
        fails(InvalidParcelException.class, () -> new Document(tracking(), null, bd("0.1"), 1));
        fails(InvalidParcelException.class, () -> new Document(tracking(), "Ana", null, 1));
        Document trimmed = new Document(" ABC0999 ", "  Ana Cruz  ", bd("0.1"), 1);
        equal(trimmed.getTrackingNumber(), "ABC0999");
        equal(trimmed.getReceiverName(), "Ana Cruz");
    }

    private static void testManager() throws Exception {
        ParcelManager manager = ParcelManager.getInstance();
        check(manager == ParcelManager.getInstance(), "Manager must be a Singleton");
        Document first = document("0.1", 1);
        Box second = box("11", 1, 1, 1);
        manager.addParcel(first);
        manager.addParcel(second);
        List<Parcel> parcels = manager.getAllParcels();
        check(parcels.get(parcels.size() - 2) == first, "First parcel retains insertion order");
        check(parcels.get(parcels.size() - 1) == second, "Second parcel retains insertion order");
        equal(first.getStatus(), ParcelStatus.PENDING);
        check(manager.findParcel(" " + first.getTrackingNumber() + " ") == first, "Lookup trims tracking");
        fails(DuplicateTrackingNumberException.class, () -> manager.addParcel(first));
        Document duplicate = new Document(first.getTrackingNumber(), "Other", bd("0.2"), 10);
        fails(DuplicateTrackingNumberException.class, () -> manager.addParcel(duplicate));
        check(manager.findParcel(first.getTrackingNumber()) == first, "Duplicate must not replace original");
        fails(UnsupportedOperationException.class, () -> parcels.clear());
        fails(InvalidParcelException.class, () -> manager.addParcel(null));
        fails(ParcelNotFoundException.class, () -> manager.findParcel("ABC9999"));
        fails(ParcelNotFoundException.class, () -> manager.markAsDelivered("ABC9999"));
        fails(ParcelNotFoundException.class,
                () -> manager.changeDeliveryService("ABC9999", new StandardDelivery()));
        manager.changeDeliveryService(first.getTrackingNumber(), new ExpressDelivery());
        decimal(first.getDeliveryFee(), "75");
        check(manager.markAsDelivered(first.getTrackingNumber()), "Pending parcel becomes delivered");
        check(!manager.markAsDelivered(first.getTrackingNumber()), "Delivery is idempotent");
        equal(first.getStatus(), ParcelStatus.DELIVERED);
        fails(ParcelAlreadyDeliveredException.class,
                () -> manager.changeDeliveryService(first.getTrackingNumber(), new SameDayDelivery()));
        fails(ParcelAlreadyDeliveredException.class,
                () -> first.changeDeliveryService(new StandardDelivery()));
        decimal(first.getDeliveryFee(), "75");
        fails(DeliveryServiceUnavailableException.class,
                () -> manager.changeDeliveryService(second.getTrackingNumber(), new SameDayDelivery()));
        decimal(second.getDeliveryFee(), "440");
    }

    private static void testConsole() throws Exception {
        String transcript = console(String.join("\n",
                "nonsense", "9", "1", "0", "3", "abc1000", "ABC1000", "", "Ana Cruz",
                "NaN", "31", "1.8", "0", "40", "no", "20", "-1", "10", "", "Keep upright",
                "4", "2", // Add the assignment example as Express.
                "1", "2", "ABC1001", "Ben", "11", "1", "1", "1", "3", "2", // Same Day retry.
                "3", "ABC1001", "3", // Rejected change returns to menu, keeping Express.
                "2", // View all.
                "1", "1", "ABC1000", "ABC1002", "Cora", "0.6", "Infinity", "0", "0.5",
                "1.5", "2147483648", "0", "101", "100", "1", // Duplicate and numeric retries.
                "3", "ABC1000", "3", // Eligible change to Same Day.
                "4", "ABC1000", "4", "ABC1000", "3", "ABC1000", // Delivered restrictions.
                "3", "ABC9999", "4", "ABC9999", "2", "5", ""));
        for (String message : new String[] {
                "Menu option must be a valid whole number!", "Menu option must be between 1 and 5!",
                "Parcel type must be between 1 and 3!", "Tracking number must be ABC followed by 4 digits!",
                "Receiver name must not be empty!", "Weight must be a valid decimal number!",
                "Weight must be greater than 0 and at most 30 kg!", "Length must be a positive integer!",
                "Width must be a valid whole number!", "Height must be a positive integer!",
                "Handling note must not be empty!", "Delivery service must be between 1 and 3!",
                "Parcel added successfully! Delivery fee: Php 330.00",
                "Parcel added successfully! Delivery fee: Php 660.00",
                "Same Day delivery is only for parcels up to 10 kg!", "Tracking number already exists!",
                "Weight must be greater than 0 and at most 0.5 kg!",
                "Number of pages must be a valid whole number!", "Number of pages must be between 1 and 100!",
                "Parcel added successfully! Delivery fee: Php 50.00",
                "Delivery service changed! New fee: Php 540.00", "Parcel delivered successfully!",
                "Parcel was already delivered.", "Parcel already delivered!", "Parcel not found!",
                "Tracking Number", "Chargeable Weight (kg)", "Delivery Fee", "Pending", "Delivered", "Goodbye!"}) {
            contains(transcript, message);
        }
        ParcelManager manager = ParcelManager.getInstance();
        Parcel fragile = manager.findParcel("ABC1000");
        decimal(fragile.getDeliveryFee(), "540");
        equal(fragile.getStatus(), ParcelStatus.DELIVERED);
        Parcel box = manager.findParcel("ABC1001");
        equal(box.getDeliveryService().getName(), "Express");
        decimal(box.getDeliveryFee(), "660");
        Parcel document = manager.findParcel("ABC1002");
        decimal(document.getChargeableWeight(), "0.5");
        decimal(document.getDeliveryFee(), "50");
        check(transcript.lines().anyMatch(line -> line.startsWith("ABC1001")
                && line.contains("Box") && line.contains("Express") && line.contains("Php 660.00")
                && line.contains("Pending")), "Table includes all box attributes and retained service");
        check(transcript.lines().anyMatch(line -> line.startsWith("ABC1000")
                && line.contains("Fragile") && line.contains("Ana Cruz") && line.contains("Same Day")
                && line.contains("Php 540.00") && line.contains("Delivered")), "Table shows final fragile state");
    }

    private static void testUnchangedDeliveryService() throws Exception {
        ParcelManager manager = ParcelManager.getInstance();
        Document parcel = document("0.1", 1);
        manager.addParcel(parcel);
        DeliveryStrategy[] services = {new StandardDelivery(), new ExpressDelivery(), new SameDayDelivery()};
        for (int index = 0; index < services.length; index++) {
            parcel.changeDeliveryService(services[index]);
            BigDecimal oldFee = parcel.getDeliveryFee();
            String transcript = console("3\n" + parcel.getTrackingNumber() + "\n" + (index + 1) + "\n5\n");
            contains(transcript, "It is already " + services[index].getName());
            contains(transcript, "Goodbye!");
            check(!transcript.contains("Delivery service changed!"),
                    "Selecting the current service must not report a change");
            check(!transcript.contains("Input ended."), "Unchanged service must return to the menu");
            check(parcel.getDeliveryService() == services[index], "Current strategy must be preserved");
            decimal(parcel.getDeliveryFee(), oldFee.toPlainString());
            equal(parcel.getStatus(), ParcelStatus.PENDING);
        }
        manager.markAsDelivered(parcel.getTrackingNumber());
        String transcript = console("3\n" + parcel.getTrackingNumber() + "\n5\n");
        contains(transcript, "Parcel already delivered!");
        check(!transcript.contains("Select delivery service:"), "Delivered parcels must reject service selection");
        check(!transcript.contains("It is already "), "Delivered-parcel message takes precedence");
    }

    private static void testEndOfInput() throws Exception {
        for (String input : new String[] {"", "1\n", "1\n1\n", "1\n1\nABC2000\n",
                "1\n1\nABC2000\nAna\n0.1\n1\n", "3\n", "4\n"}) {
            contains(console(input), "Input ended. Goodbye!");
        }
        fails(ParcelNotFoundException.class, () -> ParcelManager.getInstance().findParcel("ABC2000"));
    }

    private static String console(String input) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            new ParcelDeliverySystem(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output).run();
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static Document document(String weight, int pages) throws InvalidParcelException {
        return new Document(tracking(), "Ana", bd(weight), pages);
    }

    private static Box box(String weight, int length, int width, int height) throws InvalidParcelException {
        return new Box(tracking(), "Ben", bd(weight), length, width, height);
    }

    private static Fragile fragile(String weight, String note) throws InvalidParcelException {
        return new Fragile(tracking(), "Cora", bd(weight), 40, 20, 10, note);
    }

    private static String tracking() { return String.format("ABC%04d", nextTracking++); }
    private static BigDecimal bd(String number) { return new BigDecimal(number); }

    private static void decimal(BigDecimal actual, String expected) {
        check(actual.compareTo(bd(expected)) == 0, "Expected " + expected + ", got " + actual);
    }

    private static void equal(Object actual, Object expected) {
        check(expected.equals(actual), "Expected " + expected + ", got " + actual);
    }

    private static void contains(String text, String expected) {
        check(text.contains(expected), "Missing output: " + expected);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void fails(Class<? extends Throwable> expected, ThrowingAction action) throws Exception {
        checks++;
        try {
            action.run();
        } catch (Throwable exception) {
            if (expected.isInstance(exception)) {
                return;
            }
            throw new AssertionError("Expected " + expected.getSimpleName() + ", got " + exception, exception);
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
