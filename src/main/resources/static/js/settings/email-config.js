// email-config.js
// Handles the email notification configuration tab on the settings page.
// Depends on: jQuery, DataTables, iCheck, toastr, and the global `token` CSRF variable.

function loadDataTable() {
    $('body').getThen('/admin/settings/emailconfig').then(function (htmlContent) {
        $('#emailConfiguration').html($(htmlContent).find("#emailConfiguration"));

        var recipientsTable = $('#recipientsTable').DataTable({
            "searching": false,
            "ordering": false,
            "pageLength": 10,
            "bLengthChange": false,
            "responsive": true,
            "autoWidth": false,
            "columnDefs": [
                { "orderable": false, "searchable": false, "visible": false, "targets": 0 },
                { "orderable": false, "searchable": false, "targets": 1 }
            ],
            "language": {
                "search": "Søg",
                "lengthMenu": "_MENU_ logs per side",
                "info": "Viser _START_ til _END_ af _TOTAL_ modtagere",
                "zeroRecords": "Ingen data...",
                "infoEmpty": "",
                "infoFiltered": "(ud af _MAX_ logs)",
                "paginate": {
                    "previous": "Forrige",
                    "next": "Næste"
                }
            },
            stateSave: true,
            stateSaveCallback: function (settings, data) {
                localStorage.setItem(
                    'DataTables_' + settings.sInstance,
                    JSON.stringify(data)
                );
            },
            stateLoadCallback: function (settings) {
                return JSON.parse(localStorage.getItem('DataTables_' + settings.sInstance));
            }
        });

        $('#recipientsTable .table').each(function () {
            $(this).DataTable({
                "searching": false,
                "ordering": false,
                "pageLength": 5,
                "bLengthChange": false,
                "responsive": true,
                "autoWidth": false,
                "columnDefs": [
                    { "orderable": false, "searchable": false, "targets": 0 },
                    { "orderable": false, "searchable": false, "targets": 1 }
                ],
                "language": {
                    "search": "Søg",
                    "lengthMenu": "_MENU_ logs per side",
                    "info": "Viser _START_ til _END_ af _TOTAL_ modtagere",
                    "zeroRecords": "Ingen e-mails tilføjet",
                    "infoEmpty": "",
                    "infoFiltered": "(ud af _MAX_ logs)",
                    "paginate": {
                        "previous": "Forrige",
                        "next": "Næste"
                    }
                }
            });

            $(this).DataTable().on("draw", function () {
                initIChecks();
            });
        });

        initIChecks();
    });
}

function addSubscriber(elem) {
    var inboxEan = elem.dataset.inbox;
    var emails = [];

    $(elem).parents(".table").find("tbody > tr > td:first-child").each(function () {
        emails.push($(this).text());
    });

    var input = document.getElementById('text-input-' + inboxEan);
    var error = document.getElementById('email-error-' + inboxEan);
    var value = input.value.trim();

    input.classList.remove('is-valid', 'is-invalid');
    error.textContent = '';

    var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (value === '') {
        input.classList.add('is-invalid');
        error.textContent = 'Angiv værdien';
    } else if (!emailRegex.test(value)) {
        input.classList.add('is-invalid');
        error.textContent = 'Ugyldig emailadresse';
    } else if (emails.includes(value)) {
        input.classList.add('is-invalid');
        error.textContent = 'Allerede tildelt';
    } else {
        input.classList.add('is-valid');

        $.ajax({
            url: "/rest/admin/settings/addSubscriber",
            type: 'POST',
            headers: { 'X-CSRF-TOKEN': token },
            data: JSON.stringify({
                inboxEan: inboxEan,
                email: value
            }),
            cache: false,
            contentType: 'application/json',
            processData: false,
            success: function () {
                toastr.success(value + " tilføjet");
                loadDataTable();
            },
            error: function () {
                toastr.error("Fejl!");
            }
        });
    }
}

function removeSubscriber(elem) {
    var inboxEan = elem.dataset.inbox;
    var value = elem.dataset.email;

    $.ajax({
        url: "/rest/admin/settings/removeSubscriber",
        type: 'POST',
        headers: { 'X-CSRF-TOKEN': token },
        data: JSON.stringify({
            inboxEan: inboxEan,
            email: value
        }),
        cache: false,
        contentType: 'application/json',
        processData: false,
        success: function () {
            toastr.success(value + " blev fjernet");
            loadDataTable();
        },
        error: function () {
            toastr.error("Fejl!");
        }
    });
}

function setNegativeReceiptNotification(elem) {
    var inboxEan = elem.dataset.inbox;
    var value = $(elem).prop('checked');

    $.ajax({
        url: "/rest/admin/settings/setNegativeReceiptNotification",
        type: 'POST',
        headers: { 'X-CSRF-TOKEN': token },
        data: JSON.stringify({
            inboxEan: inboxEan,
            value: value
        }),
        cache: false,
        contentType: 'application/json',
        processData: false,
        success: function () {
            toastr.success("Opdateret!");
        },
        error: function () {
            toastr.error("Fejl!");
        }
    });
}