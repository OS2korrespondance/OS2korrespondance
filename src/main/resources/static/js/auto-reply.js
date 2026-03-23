function toggleAutoReply(elem) {
    const inboxEan = elem.dataset.inbox;
    const enabled = $(elem).prop('checked');
    const config = document.getElementById('auto-reply-config-' + inboxEan);
    const saveBtn = $(elem).closest('.ibox-content').find('button[data-url]')[0];
    const url = saveBtn ? saveBtn.dataset.url : '/rest/admin/settings/setAutoReply';
    if (enabled) {
        $(config).slideDown();
    } else {
        $(config).slideUp();
        let subject = document.getElementById('auto-reply-subject-' + inboxEan);
        if (subject) {
            subject.value = '';
        }
        let message = document.getElementById('auto-reply-message-' + inboxEan);
        if (message) {
            message.value = '';
        }
        let startDate = document.getElementById('auto-reply-start-' + inboxEan);
        if (startDate) {
            startDate.value = '';
        }
        let stopDate = document.getElementById('auto-reply-end-' + inboxEan);
        if (stopDate) {
            stopDate.value = '';
        }

        $.ajax({
            url: url,
            type: 'POST',
            headers: { 'X-CSRF-TOKEN': token },
            data: JSON.stringify({ inboxEan: inboxEan, enabled: false, subject: null, message: null, startDate: null, endDate: null }),
            contentType: 'application/json',
            processData: false,
            success: function() { toastr.success("Automatisk svar deaktiveret!"); },
            error: function() { toastr.error("Fejl!"); }
        });
    }
}

function saveAutoReply(elem) {
    const inboxEan = elem.dataset.inbox;
    const url = elem.dataset.url;
    const enabled = $(elem).closest('.ibox-content').find('.auto-reply-toggle').prop('checked');
    const subject = document.getElementById('auto-reply-subject-' + inboxEan)?.value ?? '';
    const message = document.getElementById('auto-reply-message-' + inboxEan)?.value ?? '';
    const startDate = document.getElementById('auto-reply-start-' + inboxEan)?.value || null;
    const endDate = document.getElementById('auto-reply-end-' + inboxEan)?.value || null;

    if (startDate && endDate && endDate < startDate) {
        toastr.error("Slutdato må ikke være før startdato");
        return;
    }

    $.ajax({
        url: url,
        type: 'POST',
        headers: { 'X-CSRF-TOKEN': token },
        data: JSON.stringify({
            inboxEan: inboxEan,
            enabled: enabled,
            subject: subject,
            message: message,
            startDate: startDate,
            endDate: endDate
        }),
        contentType: 'application/json',
        processData: false,
        success: function() { toastr.success("Automatisk svar gemt!"); },
        error: function() { toastr.error("Fejl!"); }
    });
}
