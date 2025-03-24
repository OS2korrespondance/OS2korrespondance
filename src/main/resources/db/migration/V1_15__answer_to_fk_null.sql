alter table mail
    drop foreign key fk_mail_answer_to;

alter table mail
    add constraint fk_mail_answer_to
        foreign key (answer_to) references mail (id)
            on delete set null;

