package dk.digitalidentity.medcommailbox.model.entity.enums;

public enum ReceiptType {
	NEGATIVE("Negativ"),
	NEGATIVE_VANS("Negativ Vans"),
	POSITIVE("Positiv");
	
	private final String displayValue;
    
    private ReceiptType(String displayValue) {
        this.displayValue = displayValue;
    }
    
    public String getDisplayValue() {
        return displayValue;
    }
}
