public class DeliveryServiceUnavailableException extends ParcelException {
    private static final long serialVersionUID = 1L;

    public DeliveryServiceUnavailableException() {
        super("Same Day delivery is only for parcels up to 10 kg!");
    }
}
