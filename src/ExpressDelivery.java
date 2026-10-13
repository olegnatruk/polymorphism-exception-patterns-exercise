import java.math.BigDecimal;

public final class ExpressDelivery implements DeliveryStrategy {
    @Override
    public String getName() { return "Express"; }

    @Override
    public BigDecimal calculateFee(Parcel parcel) {
        return parcel.getBaseFee().multiply(new BigDecimal("1.5"));
    }

    @Override
    public void validate(Parcel parcel) { }
}
