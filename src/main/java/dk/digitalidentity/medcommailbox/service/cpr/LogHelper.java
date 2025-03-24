package dk.digitalidentity.medcommailbox.service.cpr;

public class LogHelper {

	public static String SafeCPR(String cpr) {
		return cpr == null ? "" : cpr.length() <= 6 ? cpr : cpr.substring(0, 6) + "-****";
	}
}
