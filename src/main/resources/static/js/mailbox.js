$(document).ready(function() {
    mailboxService = new MailboxService();
    mailboxService.init();
});

function MailboxService() {

    const selectedIds = new Set();

    this.init = function() {
        const table = $('.table-mail').DataTable({
            "pageLength" : 25,
            "bLengthChange": false,
            "responsive" : true,
            "autoWidth": false,
            "order": [[ 7, 'desc' ]],
            "language" : {
                "search" : "Søg",
                "lengthMenu" : "_MENU_ beskeder per side",
                "info" : "Viser _START_ til _END_ af _TOTAL_ beskeder",
                "zeroRecords" : "Ingen data...",
                "infoEmpty" : "Ingen meddelelser...",
                "infoFiltered": "",
                "paginate" : {
                    "previous" : "Forrige",
                    "next" : "Næste"
                }
            },
            "ordering": true,
            "destroy":true,
            "serverSide": true,
            "ajax": {
                "contentType": "application/json",
                "url": `/rest/mail/${folder}`+ folderIdPostfix,
                "type": "POST",
                "headers": {
                    "X-CSRF-TOKEN": token
                },
                "data": function(d) {
                    return JSON.stringify(d);
                }
            },
            //Sets class on each row
            "createdRow" : (row, data, dataIndex)=>{
                row.classList.add( data.read ? 'read' : 'unread')
            },
            //Defines each column
            "columnDefs" : [
                {
                    targets: [0],
                    data: 'id',
                    searchable : false,
                    sortable : false,
                    render: (data, type, row, meta) => {
                        return `<input type="checkbox" class="i-checks" value="${data}">`;
                    }
                },
                {
                    targets: [1],
                    data: 'highPriorityMail',
                    render: (data, type, row, meta) => {
                        return data ? `<em class="fa fa-star" style="color: #E5C04D" title="Høj prioritet"></em>` : null;
                    },
                    className: 'mail-column',
                    searchable : false,
                },
                {
                    targets: [2],
                    data: 'otherParty',
                    render: (data, type, row, meta) => {
                        if (type === "display" && row.replied && row.folder === "INBOX") {
                            return `<i class="fa fa-reply"></i>&nbsp; ${data}`;
                        }
                        return data;
                    },
                    className: 'mail-column',
                },
                {
                    targets: [3],
                    data: 'patientName',
                    className: 'mail-column',
                },
                {
                    targets: [4],
                    data: 'patientCpr',
                    className: 'mail-column',
                },
                {
                    targets: [5],
                    data: 'subject',
                    className: 'mail-column',
                },
                {
                    targets: [6],
                    data: 'attachmentCount',
                    render: (data, type, row, meta) => {
                        return data && data > 0 ? `<i class="fa fa-paperclip"></i>` : '';
                    },
                    className: 'mail-column',
                    searchable : false,
                },
                {
                    targets: [7],
                    data: 'received',
                    render: (data, type, row, meta) => {
                        const date = new Date(data)
                        return type === "display" || type === "filter" ?
                        `${date.getDate()}/${date.getMonth()+1}/${date.getFullYear()} ${date.getHours()}:${date.getMinutes()}`
                        : data
                    },
                    className: 'mail-column',
                },
                {
                    targets: [8],
                    data: "receivedNegativeReceipt",
                    render: (data, type, row, meta) => {
                        if (data === true) {
                            return `<span class="label label-danger" data-toggle="tooltip" data-placement="top" title="Negativ kvittering"><i class="fa fa-fw fa-exclamation-triangle"></i></span>`
                        }
                        return '';
                    }
                }

            ]
        });

        table.on('draw', () => {
            $('.table-mail tbody .i-checks').iCheck({
                checkboxClass: 'icheckbox_square-green',
                radioClass: 'iradio_square-green',
            });

            // Restore checked state for rows on the new page
            $('.table-mail tbody .i-checks').each(function() {
                if (selectedIds.has($(this).val())) {
                    $(this).iCheck('check');
                }
            });

            // Check selectAll if every row on this page is selected
            const allChecked = $('.table-mail tbody .i-checks').length > 0 &&
                $('.table-mail tbody .i-checks').toArray().every(el => selectedIds.has($(el).val()));
            $('#selectAll').iCheck(allChecked ? 'check' : 'uncheck');

            // Track checkbox changes
            $('.table-mail tbody .i-checks').on('ifChanged', function() {
                const id = $(this).val();
                if ($(this).prop('checked')) {
                    selectedIds.add(id);
                } else {
                    selectedIds.delete(id);
                }
            });
        });

        // Select-all checkbox in header
        $('#selectAll').iCheck({
            checkboxClass: 'icheckbox_square-green',
            radioClass: 'iradio_square-green',
        });
        $('#selectAll').on('ifChanged', function() {
            const checked = $(this).prop('checked');
            $('.table-mail tbody .i-checks').each(function() {
                if (checked) {
                    selectedIds.add($(this).val());
                } else {
                    selectedIds.delete($(this).val());
                }
            });
            $('.table-mail tbody .i-checks').iCheck(checked ? 'check' : 'uncheck');
        });

        // Stop checkbox clicks from bubbling up to the row click handler
        $('.table-mail tbody').on('click', '.i-checks', (event) => {
            event.stopPropagation();
        });

        //Event listener for clicks on table rows
        table.on('click', 'tbody tr', (event)=> {
            let data = table.row(event.target.closest('tr')).data();
            location.href= '/mail/' + data.id;
        })

        // Init move modal folder selector
        $('#bulkMoveTo').on('change', function() {
            if ($(this).val() == createFolderId) {
                $('#bulkNewFolderSection').prop('hidden', false);
            } else {
                $('#bulkNewFolderSection').prop('hidden', true);
                $('#bulkFolderName').val('');
            }
        });

    }

    const getSelectedIds = () => {
        return Array.from(selectedIds);
    }

    const confirmAndPost = (swalOptionsFactory, url, body, successMessage, errorMessage) => {
        const ids = getSelectedIds();
        if (ids.length === 0) {
            return;
        }

        swal(swalOptionsFactory(ids.length), async (isConfirmed) => {
            if (!isConfirmed) {
                return;
            }
            const response = await fetch(url, {
                method: 'POST',
                headers: { 'X-CSRF-TOKEN': token, 'Content-Type': 'application/json' },
                body: JSON.stringify({ ids: ids.map(Number), ...body })
            });
            if (!response.ok) {
                toastr.warning(errorMessage);
                return;
            }
            toastr.success(successMessage);
            location.reload();
        });
    }

    this.bulkDelete = () => {
        confirmAndPost(
            (count) => ({
                html: true,
                title: 'Slet beskeder',
                text: `Er du sikker på at du vil flytte ${count} besked(er) til slettede beskeder?<br/>De vil blive slettet permanent om ${daysBeforeDeletion} dage.`,
                type: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#DD6B55',
                confirmButtonText: 'Fortsæt',
                cancelButtonText: 'Annuller',
                closeOnConfirm: true,
                closeOnCancel: true
            }),
            '/rest/mails/bulk/delete', { delete: true }, 'Beskeder slettet', 'Noget gik galt ved sletning af beskeder'
        );
    }

    this.bulkArchive = () => {
        confirmAndPost(
            (count) => ({
                html: true,
                title: 'Arkiver beskeder',
                text: `Er du sikker på at du vil arkivere ${count} besked(er)?`,
                type: 'info',
                showCancelButton: true,
                confirmButtonColor: '#1AB394',
                confirmButtonText: 'Fortsæt',
                cancelButtonText: 'Annuller',
                closeOnConfirm: true,
                closeOnCancel: true
            }),
            '/rest/mails/bulk/archive', { archive: true }, 'Beskeder arkiveret', 'Noget gik galt ved arkivering af beskeder'
        );
    }

    this.bulkUnarchive = () => {
        confirmAndPost(
            (count) => ({
                html: true,
                title: 'Flyt til indbakke',
                text: `Er du sikker på at du vil flytte ${count} besked(er) til indbakken?`,
                type: 'info',
                showCancelButton: true,
                confirmButtonColor: '#1AB394',
                confirmButtonText: 'Fortsæt',
                cancelButtonText: 'Annuller',
                closeOnConfirm: true,
                closeOnCancel: true
            }),
            '/rest/mails/bulk/archive', { archive: false }, 'Beskeder flyttet til indbakke', 'Noget gik galt ved flytning af beskeder'
        );
    }

    this.bulkUndelete = () => {
        confirmAndPost(
            (count) => ({
                html: true,
                title: 'Gendan beskeder',
                text: `Er du sikker på at du vil gendanne ${count} besked(er)?`,
                type: 'info',
                showCancelButton: true,
                confirmButtonColor: '#1AB394',
                confirmButtonText: 'Fortsæt',
                cancelButtonText: 'Annuller',
                closeOnConfirm: true,
                closeOnCancel: true
            }),
            '/rest/mails/bulk/delete', { delete: false }, 'Beskeder gendannet', 'Noget gik galt ved gendannelse af beskeder'
        );
    }

    this.openBulkMoveModal = () => {
        $('#bulkMoveMailModal').modal('show');
    }

    this.bulkMove = async () => {
        const ids = getSelectedIds();
        if (ids.length === 0) {
            return;
        }

        const folderId = $('#bulkMoveTo').val();
        const newFolderName = $('#bulkFolderName').val();

        if (folderId == createFolderId && (!newFolderName || newFolderName.trim() === '')) {
            toastr.warning('Angiv venligst et mappenavn');
            return;
        }

        $('#bulkMoveMailModal').modal('hide');

        const response = await fetch('/rest/mails/bulk/move', {
            method: 'POST',
            headers: { 'X-CSRF-TOKEN': token, 'Content-Type': 'application/json' },
            body: JSON.stringify({ ids: ids.map(Number), folderId: parseInt(folderId), newFolderName: newFolderName })
        });
        if (!response.ok) {
            toastr.error('Fejl ved flytning af beskeder');
            return;
        }
        toastr.success('Beskeder flyttet');
        location.reload();
    }

    this.onFolderDelete = (customFolderId)=> {
        //warn user
        swal({
            html: true,
            title : "Slet mappe",
            text : "Mappen vil blive slettet og alle beskeder i den vil blive flyttet til indbakken. Er du sikker på at du vil fortsætte?",
            type : "info",
            showCancelButton : true,
            confirmButtonColor : "#DD6B55",
            confirmButtonText : 'Fortsæt',
            cancelButtonText : 'Annuller',
            closeOnConfirm : true,
            closeOnCancel : true
        },
        (isConfirmed) => {
            if (isConfirmed) {
                this.deleteFolder(customFolderId)
            }
        });
    }

    this.deleteFolder = async (customFolderId)=> {
        const url = `${folderRestUrl}/delete/${customFolderId}`

        const response = await fetch(url, {
            "method": "DELETE",
            "headers": {
                "X-CSRF-TOKEN": token
            },
        })

        if (!response.ok) {
            toastr.warning('Noget gik galt i sletningen af mappen');
            console.error("Error while attempting to delete folder with id: "+customFolderId)
            throw new Error(response.statusText)
        }

        toastr.success("Mappen er slettet!");
        location.href= '/mailbox/INBOX';
    }
}
