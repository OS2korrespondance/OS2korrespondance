// monitoring.js
// Loaded after the monitoring fragment is injected into the DOM.
// Depends on: jQuery, DataTables, toastr, and the global `token` CSRF variable from settings.html

function loadMonitoringConfig() {
    $('body').getThen('/admin/settings/monitoringconfig').then(function (htmlContent) {
        $('#monitoringConfiguration').html($(htmlContent).find("#monitoringConfiguration"));
        initMonitoringTables();
        initMonitoringListeners();
        initAlarmIntervalSelect();
    });
}

function initMonitoringTables() {
    var dtOptions = {
        "searching": false,
        "ordering": false,
        "pageLength": 5,
        "bLengthChange": false,
        "responsive": true,
        "autoWidth": false,
        "columnDefs": [
            { "orderable": false, "searchable": false, "targets": 0 },
            { "orderable": false, "searchable": false, "targets": 1, "width": "40px" }
        ],
        "language": {
            "search": "Søg",
            "info": "Viser _START_ til _END_ af _TOTAL_ modtagere",
            "zeroRecords": "Ingen e-mails tilføjet",
            "infoEmpty": "",
            "paginate": {
                "previous": "Forrige",
                "next": "Næste"
            }
        }
    };

    $('#alarmSubscribersTable').DataTable(dtOptions);
    $('#contactSubscribersTable').DataTable(dtOptions);
}

function initMonitoringListeners() {
    // Add alarm subscriber
    var btnAddAlarm = document.getElementById('btn-add-alarm');
    if (btnAddAlarm) {
        btnAddAlarm.addEventListener('click', function () {
            addMonitoringSubscriber('alarm-email-input', 'alarm-email-error', 'AUTOMATIC_ALARM');
        });
    }

    // Add contact subscriber
    var btnAddContact = document.getElementById('btn-add-contact');
    if (btnAddContact) {
        btnAddContact.addEventListener('click', function () {
            addMonitoringSubscriber('contact-email-input', 'contact-email-error', 'MANUAL_CONTACT');
        });
    }

    // Remove alarm subscribers (delegated)
    $(document).on('click', '.btn-remove-alarm', function () {
        removeMonitoringSubscriber($(this).data('email'), 'AUTOMATIC_ALARM');
    });

    // Remove contact subscribers (delegated)
    $(document).on('click', '.btn-remove-contact', function () {
        removeMonitoringSubscriber($(this).data('email'), 'MANUAL_CONTACT');
    });

    // Enter key support on inputs
    var alarmInput = document.getElementById('alarm-email-input');
    if (alarmInput) {
        alarmInput.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                addMonitoringSubscriber('alarm-email-input', 'alarm-email-error', 'AUTOMATIC_ALARM');
            }
        });
    }

    var contactInput = document.getElementById('contact-email-input');
    if (contactInput) {
        contactInput.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                addMonitoringSubscriber('contact-email-input', 'contact-email-error', 'MANUAL_CONTACT');
            }
        });
    }
}

function initAlarmIntervalSelect() {
    var select = document.getElementById('alarmIntervalSelect');
    if (!select) return;

    // Set current value
    var current = select.dataset.current;
    if (current) {
        select.value = current;
    }

    select.addEventListener('change', function () {
        var minutes = parseInt(this.value, 10);
        $.ajax({
            url: "/rest/admin/monitoring/setAlarmInterval",
            type: 'POST',
            headers: { 'X-CSRF-TOKEN': token },
            data: JSON.stringify({ minutes: minutes }),
            contentType: 'application/json',
            success: function () {
                toastr.success("Alarm interval opdateret");
            },
            error: function () {
                toastr.error("Fejl ved opdatering af alarm interval");
            }
        });
    });
}

function addMonitoringSubscriber(inputId, errorId, type) {
    var input = document.getElementById(inputId);
    var error = document.getElementById(errorId);
    var value = input.value.trim();

    input.classList.remove('is-valid', 'is-invalid');
    error.textContent = '';

    var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (value === '') {
        input.classList.add('is-invalid');
        error.textContent = 'Angiv en e-mailadresse';
        return;
    }

    if (!emailRegex.test(value)) {
        input.classList.add('is-invalid');
        error.textContent = 'Ugyldig e-mailadresse';
        return;
    }

    $.ajax({
        url: "/rest/admin/monitoring/addSubscriber",
        type: 'POST',
        headers: { 'X-CSRF-TOKEN': token },
        data: JSON.stringify({ type: type, email: value }),
        contentType: 'application/json',
        success: function () {
            toastr.success(value + " tilføjet");
            loadMonitoringConfig();
        },
        error: function () {
            toastr.error("Fejl ved tilføjelse");
        }
    });
}

function removeMonitoringSubscriber(email, type) {
    $.ajax({
        url: "/rest/admin/monitoring/removeSubscriber",
        type: 'POST',
        headers: { 'X-CSRF-TOKEN': token },
        data: JSON.stringify({ type: type, email: email }),
        contentType: 'application/json',
        success: function () {
            toastr.success(email + " blev fjernet");
            loadMonitoringConfig();
        },
        error: function () {
            toastr.error("Fejl ved fjernelse");
        }
    });
}