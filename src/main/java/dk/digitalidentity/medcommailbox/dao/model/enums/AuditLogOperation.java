package dk.digitalidentity.medcommailbox.dao.model.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public enum AuditLogOperation {
	USER_LOGGED_IN("Bruger har logget ind"),
	MAIL_DELETE("Besked slettet"),
	MAIL_UNDELETE("Besked ikke længere slettet"),
	MAIL_SENT("Besked afsendt"),
	
	RECIPIENT_CREATED("Modtager oprettet"),
	RECIPIENT_DELETED("Modtager slettet"),
	RECIPIENT_CHANGE_NAME("Modtager opdateret");
	private String message;

	private AuditLogOperation(String message) {
		this.message = message;
	}

	public static List<AuditLogOperation> getSorted() {
		List<AuditLogOperation> result = new ArrayList<>();
		Collections.addAll(result, AuditLogOperation.values());
		result.sort((a, b) -> a.getMessage().compareToIgnoreCase(b.getMessage()));
		return result;
	}
}
