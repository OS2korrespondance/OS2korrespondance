function loadAutoDeleteConfig() {
    $('body').getThen('/admin/settings/autodeleteconfig').then(function (htmlContent) {
        $('#autoDeleteConfiguration').html($(htmlContent).find("#autoDeleteConfiguration"));
        initIChecks();
    });
}

function toggleAutoDelete(elem) {
    const inboxEan = elem.dataset.inbox;
    const enabled = $(elem).prop('checked');
    const config = document.getElementById('auto-delete-config-' + inboxEan);
    const saveBtn = $(elem).closest('.ibox-content').find('button[data-url]')[0];
    const url = saveBtn ? saveBtn.dataset.url : '/rest/admin/settings/setAutoDelete';
    if (enabled) {
        $(config).slideDown();
    } else {
        $(config).slideUp();
        let days = document.getElementById('auto-delete-days-' + inboxEan);
        if (days) {
            days.value = '';
        }

        $.ajax({
            url: url,
            type: 'POST',
            headers: { 'X-CSRF-TOKEN': token },
            data: JSON.stringify({ inboxEan: inboxEan, enabled: false, autoDeleteDays: null }),
            contentType: 'application/json',
            processData: false,
            success: function() { toastr.success("Automatisk sletning deaktiveret!"); },
            error: function() { toastr.error("Fejl!"); }
        });
    }
}

function saveAutoDelete(elem) {
    const inboxEan = elem.dataset.inbox;
    const url = elem.dataset.url;
    const enabled = $(elem).closest('.ibox-content').find('.auto-delete-toggle').prop('checked');
    const days = document.getElementById('auto-delete-days-' + inboxEan)?.value ?? '';

    $.ajax({
        url: url,
        type: 'POST',
        headers: { 'X-CSRF-TOKEN': token },
        data: JSON.stringify({
            inboxEan: inboxEan,
            enabled: enabled,
            autoDeleteDays: days
        }),
        contentType: 'application/json',
        processData: false,
        success: function() { toastr.success("Automatisk sletning gemt!"); },
        error: function() { toastr.error("Fejl!"); }
    });
}
