/** Base class for recoverable parcel operations and validation failures. */
public class ParcelException extends Exception {
    private static final long serialVersionUID = 1L;

    public ParcelException(String message) {
        super(message);
    }
}
