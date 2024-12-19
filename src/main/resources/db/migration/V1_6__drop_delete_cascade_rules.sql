alter table mail drop constraint fk_mail_sender;
alter table mail drop constraint fk_mail_recipient;
alter table mail drop constraint fk_mail_answer_to;
alter table mail drop constraint fk_mail_patient;

alter table mail add constraint fk_mail_sender FOREIGN KEY (sender_id) REFERENCES recipient(id);
alter table mail add CONSTRAINT fk_mail_recipient FOREIGN KEY (recipient_id) REFERENCES recipient(id);
alter table mail add CONSTRAINT fk_mail_answer_to FOREIGN KEY (answer_to) REFERENCES mail(id);
alter table mail add CONSTRAINT fk_mail_patient FOREIGN KEY (patient_id) REFERENCES patient(id);
