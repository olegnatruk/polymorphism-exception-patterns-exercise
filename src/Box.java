import java.math.BigDecimal;
import java.math.RoundingMode;

public class Box extends Parcel {
    private final int length;
    private final int width;
    private final int height;

    public Box(String trackingNumber, String receiverName, BigDecimal weight,
               int length, int width, int height) throws InvalidParcelException {
        this(trackingNumber, receiverName, weight, length, width, height, new BigDecimal("50"));
    }

    protected Box(String trackingNumber, String receiverName, BigDecimal weight,
                  int length, int width, int height, BigDecimal maximumWeight)
            throws InvalidParcelException {
        super(trackingNumber, receiverName, weight, maximumWeight);
        this.length = Validation.integer(length, 1, Integer.MAX_VALUE, "Length");
        this.width = Validation.integer(width, 1, Integer.MAX_VALUE, "Width");
        this.height = Validation.integer(height, 1, Integer.MAX_VALUE, "Height");
    }

    public final int getLength() { return length; }
    public final int getWidth() { return width; }
    public final int getHeight() { return height; }

    @Override
    public String getType() { return "Box"; }

    @Override
    public BigDecimal getChargeableWeight() {
        // BigDecimal multiplication avoids integer overflow and premature rounding.
        BigDecimal volume = BigDecimal.valueOf(length).multiply(BigDecimal.valueOf(width))
                .multiply(BigDecimal.valueOf(height));
        BigDecimal roundedVolumetricWeight = volume.divide(new BigDecimal("3500"),
                0, RoundingMode.CEILING);
        return getWeight().setScale(0, RoundingMode.CEILING).max(roundedVolumetricWeight);
    }

    @Override
    public BigDecimal getBaseFee() {
        return new BigDecimal("40").multiply(getChargeableWeight());
    }
}
