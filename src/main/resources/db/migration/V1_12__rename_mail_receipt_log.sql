
rename table mail_receipt_log to receipt_log;
alter table receipt_log
    change mail_envelope_identifier envelope_identifier varchar(14) null;

alter table receipt_log
    change mail_letter_identifier letter_identifier varchar(14) null;

alter table receipt_log
    change incomming_mail incomming tinyint(1) default 0 not null;
