public class ParcelNotFoundException extends ParcelException {
    private static final long serialVersionUID = 1L;

    public ParcelNotFoundException() {
        super("Parcel not found!");
    }
}
