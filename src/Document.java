import java.math.BigDecimal;

public final class Document extends Parcel {
    private final int numberOfPages;

    public Document(String trackingNumber, String receiverName, BigDecimal weight,
                    int numberOfPages) throws InvalidParcelException {
        super(trackingNumber, receiverName, weight, new BigDecimal("0.5"));
        this.numberOfPages = Validation.integer(numberOfPages, 1, 100, "Number of pages");
    }

    public int getNumberOfPages() { return numberOfPages; }

    @Override
    public String getType() { return "Document"; }

    @Override
    public BigDecimal getChargeableWeight() { return getWeight(); }

    @Override
    public BigDecimal getBaseFee() { return new BigDecimal("50"); }
}
