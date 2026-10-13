import java.math.BigDecimal;

public final class SameDayDelivery implements DeliveryStrategy {
    @Override
    public String getName() { return "Same Day"; }

    @Override
    public BigDecimal calculateFee(Parcel parcel) {
        return parcel.getBaseFee().multiply(new BigDecimal("2")).add(new BigDecimal("100"));
    }

    @Override
    public void validate(Parcel parcel) throws DeliveryServiceUnavailableException {
        if (parcel.getChargeableWeight().compareTo(BigDecimal.TEN) > 0) {
            throw new DeliveryServiceUnavailableException();
        }
    }
}
