public enum ParcelStatus {
    PENDING("Pending"), DELIVERED("Delivered");

    private final String label;

    ParcelStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
