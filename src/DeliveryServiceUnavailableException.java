public class DeliveryServiceUnavailableException extends ParcelException {
    public DeliveryServiceUnavailableException() {
        super("Same Day delivery is only for parcels up to 10 kg!");
    }
}
