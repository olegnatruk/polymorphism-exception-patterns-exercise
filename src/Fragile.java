import java.math.BigDecimal;

public final class Fragile extends Box {
    private final String handlingNote;

    public Fragile(String trackingNumber, String receiverName, BigDecimal weight,
                   int length, int width, int height, String handlingNote)
            throws InvalidParcelException {
        super(trackingNumber, receiverName, weight, length, width, height, new BigDecimal("30"));
        this.handlingNote = Validation.text(handlingNote, "Handling note");
    }

    public String getHandlingNote() { return handlingNote; }

    @Override
    public String getType() { return "Fragile"; }

    @Override
    public BigDecimal getBaseFee() {
        return super.getBaseFee().add(new BigDecimal("100"));
    }
}
