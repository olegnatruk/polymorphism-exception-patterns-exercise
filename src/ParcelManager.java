import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Singleton: every operation shares one insertion-ordered parcel collection. */
public final class ParcelManager {
    private static final ParcelManager INSTANCE = new ParcelManager();
    private final Map<String, Parcel> parcels = new LinkedHashMap<>();

    private ParcelManager() { }

    public static ParcelManager getInstance() { return INSTANCE; }

    public void checkTrackingAvailable(String trackingNumber) throws ParcelException {
        String validated = Validation.tracking(trackingNumber);
        if (parcels.containsKey(validated)) {
            throw new DuplicateTrackingNumberException();
        }
    }

    public void addParcel(Parcel parcel) throws ParcelException {
        if (parcel == null) {
            throw new InvalidParcelException("Parcel must not be empty!");
        }
        checkTrackingAvailable(parcel.getTrackingNumber());
        parcels.put(parcel.getTrackingNumber(), parcel);
    }

    public List<Parcel> getAllParcels() { return List.copyOf(parcels.values()); }

    public Parcel findParcel(String trackingNumber) throws ParcelNotFoundException {
        Parcel parcel = parcels.get(trackingNumber == null ? null : trackingNumber.strip());
        if (parcel == null) {
            throw new ParcelNotFoundException();
        }
        return parcel;
    }

    public void changeDeliveryService(String trackingNumber, DeliveryStrategy service)
            throws ParcelException {
        findParcel(trackingNumber).changeDeliveryService(service);
    }

    public boolean markAsDelivered(String trackingNumber) throws ParcelNotFoundException {
        return findParcel(trackingNumber).markDelivered();
    }
}
