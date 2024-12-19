package dk.digitalidentity.medcommailbox.dao.model.enums;

public enum EpisodeOfCareStatusCode {
	inaktiv("Inaktiv"), 
	indlagt("Indlagt"), 
	ambulant("Ambulant"), 
	doed("Død"), 
	ambulant_roentgen("Ambulant røntgen");
	
	private final String displayValue;
    
    private EpisodeOfCareStatusCode(String displayValue) {
        this.displayValue = displayValue;
    }
    
    public String getDisplayValue() {
        return displayValue;
    }
}
