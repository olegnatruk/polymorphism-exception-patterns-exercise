import java.math.BigDecimal;

/** Common state; subclass overrides supply parcel-specific weights and base fees. */
public abstract class Parcel {
    private final String trackingNumber;
    private final String receiverName;
    private final BigDecimal weight;
    private DeliveryStrategy deliveryService = new StandardDelivery();
    private ParcelStatus status = ParcelStatus.PENDING;

    protected Parcel(String trackingNumber, String receiverName, BigDecimal weight,
                     BigDecimal maximumWeight) throws InvalidParcelException {
        this.trackingNumber = Validation.tracking(trackingNumber);
        this.receiverName = Validation.receiverName(receiverName);
        this.weight = Validation.weight(weight, maximumWeight);
    }

    public abstract String getType();
    public abstract BigDecimal getChargeableWeight();
    public abstract BigDecimal getBaseFee();

    public final String getTrackingNumber() { return trackingNumber; }
    public final String getReceiverName() { return receiverName; }
    public final BigDecimal getWeight() { return weight; }
    public final DeliveryStrategy getDeliveryService() { return deliveryService; }
    public final ParcelStatus getStatus() { return status; }

    public final BigDecimal getDeliveryFee() {
        return deliveryService.calculateFee(this);
    }

    public final void changeDeliveryService(DeliveryStrategy service) throws ParcelException {
        ensurePending();
        if (service == null) {
            throw new InvalidParcelException("Delivery service must not be empty!");
        }
        // Validate first: a rejected strategy never replaces the existing one.
        service.validate(this);
        deliveryService = service;
    }

    final void ensurePending() throws ParcelAlreadyDeliveredException {
        if (status == ParcelStatus.DELIVERED) {
            throw new ParcelAlreadyDeliveredException();
        }
    }

    final boolean markDelivered() {
        if (status == ParcelStatus.DELIVERED) {
            return false;
        }
        status = ParcelStatus.DELIVERED;
        return true;
    }
}
