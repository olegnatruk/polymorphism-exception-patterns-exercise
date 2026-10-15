public class DuplicateTrackingNumberException extends ParcelException {
    private static final long serialVersionUID = 1L;

    public DuplicateTrackingNumberException() {
        super("Tracking number already exists!");
    }
}
