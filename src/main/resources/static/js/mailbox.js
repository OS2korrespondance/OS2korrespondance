$(document).ready(function() {
    mailboxService = new MailboxService();
    mailboxService.init();
});

function MailboxService() {


    this.init = function() {
        const table = $('.table-mail').DataTable({
            "pageLength" : 25,
            "bLengthChange": false,
            "responsive" : true,
            "autoWidth": false,
            "order": [[ 6, 'desc' ]],
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
                    visible : false,
                    searchable : false,
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

            ]
        });

        //Event listener for clicks on table rows
        table.on('click', 'tbody tr', (event)=> {
            let data = table.row(event.target.closest('tr')).data();
            location.href= '/mail/' + data.id;
        })

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
