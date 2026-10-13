import java.math.BigDecimal;

/** Strategy: the parcel delegates delivery pricing to a replaceable service. */
public interface DeliveryStrategy {
    String getName();
    BigDecimal calculateFee(Parcel parcel);
    void validate(Parcel parcel) throws DeliveryServiceUnavailableException;
}
