/** Base class for recoverable parcel operations and validation failures. */
public class ParcelException extends Exception {
    public ParcelException(String message) {
        super(message);
    }
}
