
DROP VIEW IF EXISTS view_mailbox;

CREATE OR REPLACE VIEW view_mailbox AS SELECT
    m.id,
    m.high_priority_mail,
    m.original_folder,
    m.associated_identifier,
    m.subject,
    COUNT(a.id) AS attachment_count,
    COALESCE(m.received, m.sent) as received,
    IF(STRCMP(m.original_folder,"SENT") = 0, rr.short_organisation_name, rs.short_organisation_name) AS other_party,
    m.folder,
    m.received_negative_receipt,
    m.inbox_folder_id,
    m.read_mail,
    p.cpr AS patient_cpr,
    p.name AS patient_name
  FROM mail m
  LEFT JOIN reference a ON m.id = a.mail_id
  LEFT JOIN recipient rs ON rs.id = m.sender_id
  LEFT JOIN recipient rr ON rr.id = m.recipient_id
  LEFT JOIN patient p ON p.id = m.patient_id
  WHERE m.draft = 0
  GROUP BY
    m.id,
    m.high_priority_mail,
    m.original_folder,
    m.associated_identifier,
    m.subject,
    m.received,
    m.sent,
    rs.short_organisation_name,
    rr.short_organisation_name,
    m.folder,
    m.received_negative_receipt,
    m.inbox_folder_id,
    m.read_mail,
    p.cpr,
    p.name
