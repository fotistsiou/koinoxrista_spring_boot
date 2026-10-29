package gr.fotistsou.koinoxrista.enums;

public enum SplitRule {
    EQUAL_SPLIT("Ισόποσα"),
    METER_BASED("Με μετρητές");

    private final String label;

    SplitRule(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}