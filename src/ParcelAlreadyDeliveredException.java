public class ParcelAlreadyDeliveredException extends ParcelException {
    private static final long serialVersionUID = 1L;

    public ParcelAlreadyDeliveredException() {
        super("Parcel already delivered!");
    }
}
