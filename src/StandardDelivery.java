import java.math.BigDecimal;

public final class StandardDelivery implements DeliveryStrategy {
    @Override
    public String getName() { return "Standard"; }

    @Override
    public BigDecimal calculateFee(Parcel parcel) { return parcel.getBaseFee(); }

    @Override
    public void validate(Parcel parcel) { }
}
