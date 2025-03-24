package dk.digitalidentity.medcommailbox.datatables.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "view_mailbox")
public class MailboxDatatableDTO {
	@Id
	@Column
	private long id;


	@Column
	private boolean highPriorityMail;

	@Column
	private String originalFolder;

	@Column
	private String associatedIdentifier;

	@Column
	private String subject;

	@Column
	private int attachmentCount;

	@Column
	private LocalDateTime received;

	@Column
	private String otherParty;

	@Column
	private String folder;

	@Column
	private boolean receivedNegativeReceipt;

	@Column
	private Long inboxFolderId;

	@Column(name="read_mail")
	private boolean isRead;

	@Column
	private String patientCpr;

	@Column
	private String patientName;
}
