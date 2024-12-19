package dk.digitalidentity.medcommailbox.dao.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import dk.digitalidentity.medcommailbox.dao.model.enums.Folder;
import dk.digitalidentity.medcommailbox.dao.model.enums.MedicalSpecialityCodeType;
import dk.digitalidentity.medcommailbox.dao.model.enums.Status;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mail")
public class Mail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	private String subject;
	
	@Column
	private String content;
	
	@Column
	@Enumerated(EnumType.STRING)
	private Folder folder;
	
	@Column
	@Enumerated(EnumType.STRING)
	private Status status;
	
	@Column
	private LocalDateTime sent;
	
	@Column
	private LocalDateTime received;
	
	@OneToMany(mappedBy = "mail", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<Reference> references = new ArrayList<>();
	
	@Column(name = "read_mail")
	private boolean read;
	
	@Column
	private boolean draft;
	
	@Column
	private LocalDateTime created;
	
	@Column(name = "s3_file_key")
	private String s3FileKey;
	
	@Column(name = "high_priority_mail")
	private boolean highPriority;
	
	@Column
	private String envelopeIdentifier;
	
	@Column
	private String letterIdentifier;
	
	@Column
	private boolean receivedNegativeReceipt;
	
	@Column
	private String negativeReceiptReason;
	
	@Column
	private String senderDepartmentName;
	
	@Column
	@Enumerated(EnumType.STRING)
	private MedicalSpecialityCodeType senderMedicalSpecialityCode;

	@Column
	private LocalDate deletedDate;

	@Column
	@Enumerated(EnumType.STRING)
	private Folder originalFolder;

	@Column
	private String associatedIdentifier;

	@Column
	private String caseId;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "binary_id", referencedColumnName = "id")
	private BinaryMessage binaryMessage;

	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
	@JoinColumn(name = "patient_id")
	private Patient patient;
	
	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
	@JoinColumn(name = "sender_id")
	private Recipient sender;
	
	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
	@JoinColumn(name = "recipient_id")
	private Recipient recipient;
	
	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
	@JoinColumn(name = "answer_to")
	private Mail answerTo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inbox_folder_id")
	private InboxFolder inboxFolder;

}
