public class DuplicateTrackingNumberException extends ParcelException {
    public DuplicateTrackingNumberException() {
        super("Tracking number already exists!");
    }
}
