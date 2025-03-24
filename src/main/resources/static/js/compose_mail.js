

$(document).ready(function() {
    mailService = new MailService();
    mailService.init();
    templateService = new TemplateService()

    //Init cpr lookup function
    const cprInput = document.getElementById('patientCpr')
    cprInput.addEventListener('keyup', (event)=> mailService.onCprChange(event))

    const messageTemplateSelect = document.getElementById('messageTemplateSelect')
    messageTemplateSelect.addEventListener('change', (event)=> templateService.onTemplateSelectionChange(messageTemplateSelect))

});

function MailService() {
    this.init = function() {
        $('#content').keyup(function() {
            var characterCount = $(this).val().length;
            $("#counter").text(characterCount);
        });

        var characterCount = $('#content').val().length;
        $("#counter").text(characterCount);


        $('#fileUpload').on('change', function() {
            var file = this.files[0];

            if (file.size > (100 * 1024 * 1024)) {
                this.value = ""; // clear selected file
                $("#modal-attachment").modal("toggle");

                setTimeout(function() {
                    toastr.warning('Den valgte fil er for stor (max 100 MB)');
                }, 500);

                return;
            }

            var filename = file.name;
            var formData = new FormData($('#uploadAttachmentForm')[0]);
            $.ajax({
                url: "/rest/mails/" + newMailId + "/attachment/add",
                type: 'POST',
                headers: {
                    'X-CSRF-TOKEN': token
                },
                data: formData,
                cache: false,
                contentType: false,
                processData: false,
                success: function(data, textStatus, jQxhr) {
                    if (data != '') {
                        var ulElement = $('#attachments');
                        var txtHtml  = '<li id="attachment-' + data + '">\n';
                        txtHtml += '<a style="color: black;" title="Slet vedhæftning" onclick="mailService.removeAttachment(' + data + ');"><em class="fa fa-fw fa-remove"></em></a>\n';
                        txtHtml += '<span>' + filename + '</span>\n';
                        txtHtml += '</li>';

                        ulElement.append(txtHtml);
                    }
                },
                error: defaultErrorHandler
            });
        });




    }

    this.removeAttachment = function(id) {
        $.ajax({
            url: "/rest/mails/" + newMailId + "/attachment/" + id + "/delete",
            type: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            success: function(data, textStatus, jQxhr) {
                $('#attachment-' + id).remove();
            },
            error: defaultErrorHandler
        });
    }

    this.send = function() {
        var recipient = "";
        var patientName = "";
        var patientCpr = "";
        var subject = "";
        var senderIdentifier = "";
        if (!answer) {
            recipient = $("#recipient").val();
            patientName = $("#patientName").val();
            patientCpr = $("#patientCpr").val();
            subject = $("#subject").val();
            senderIdentifier = $("#sender").val();
        }

        $.ajax({
            url: "/rest/mails/" + newMailId + "/send",
            type : 'post',
            contentType: 'application/json',
            headers: {
                'X-CSRF-TOKEN': token
            },
            data : JSON.stringify({
                subject : subject,
                content : $("#content").val(),
                recipientEan : recipient,
                answer : answer,
                patientStatus: "inaktiv",
                patientName: patientName,
                patientCpr: patientCpr,
                highPriority: $("#priority").prop("checked"),
                senderIdentifier: senderIdentifier,
                caseId: caseId
            }),
            success: function(data, textStatus, jQxhr) {
                if (data == null || data == "") {
                    toastr.success("Beskeden er sendt!");
                    setTimeout(() => {window.location.href="/mailbox/INBOX"}, 1000);
                } else {
                    toastr.success("Beskeden er sendt! " + data);
                    setTimeout(() => {window.location.href="/mailbox/INBOX"}, 5000);
                }

            },
            error: defaultErrorHandler
        });
    }

    this.onCprChange = async (event)=> {
        const cprValue = event.target.value
        if (!cprValue || cprValue.length !== 10) {
            return;
        }

        const url = cprLookupUrl  + cprValue
        const response = await fetch (url, {
            headers: {
                "Content-Type": "application/json",
                'X-CSRF-TOKEN': token
            },
        })

        if(!response.ok) {
            //fail silently...?
            return
        }

        const nameInput = document.getElementById('patientName')
        nameInput.value = await response.text()
    }
}

class TemplateService {

    constructor(){}

    onSaveTemplateModalOpen(){
        this,this.resetSaveTemplateform()
        $("#saveTemplateModal").modal("show");
    }

    onSaveTemplate(){
        const subjectInput = document.getElementById('subject')
        const highPriorityInput= document.getElementById('priority')
        const contentInput= document.getElementById('content')

        const nameInput = document.getElementById('templateNameInput')

        const subject = subjectInput.value || ''
        const isHighPriority = highPriorityInput.checked || false
        const content = contentInput.value || ''
        const name = nameInput.value

        if (!name) {
            const errorElement = document.getElementById('templateNameError')
            errorElement.classList.remove('invisible')
            return
        }

        this.#postTemplate({
            name: name,
            subject : subject,
            isHighPriority : isHighPriority,
            content : content
        })


        $("#saveTemplateModal").modal("hide");
    }

    resetSaveTemplateform() {
        const nameInput = document.getElementById('templateNameInput')
        nameInput.value = ''
        const errorElement = document.getElementById('templateNameError')
        errorElement.classList.add('invisible')
    }



    async #postTemplate(templateData){
        const url = `/${restUrl}/template/save`

        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                'X-CSRF-TOKEN': token
            },
            body : JSON .stringify( templateData)
        })

        if (!response.ok) {
            console.error(response.statusText)
            errorHandler("Fejl! Skabelonen blev ikke gemt")
        }

        toastr.success("Skabelon gemt!");
    }

    onTemplateSelectionChange(selectElement) {

        //get template id
        const selectedValues = [...selectElement.selectedOptions].map( option => option.value)
        const templateId = selectedValues.length > 0 ? selectedValues[0] : null
        if (!templateId) {return;}

        const contentInput= document.getElementById('content')

        console.log(contentInput.value)

        //check if the template would overwrite existing input
        if (contentInput.value ) {
            //if so warn the user
            swal({
                html: true,
                title : "Brug skabelon",
                text : "Skabelon vil overskrive eksisterende emne og besked. Er du sikker på at du vil fortsætte?",
                type : "info",
                showCancelButton : true,
                confirmButtonColor : "#DD6B55",
                confirmButtonText : 'Fortsæt',
                cancelButtonText : 'Annuller',
                closeOnConfirm : true,
                closeOnCancel : true
            },
            (isConfirmed) => {
                console.log('confirmation', isConfirmed)
                if (isConfirmed) {
                    this.#fetchTemplateContent(templateId)

                }
            });

        } else {
            this.#fetchTemplateContent(templateId)
        }
    }

    async #fetchTemplateContent(templateId) {
        const subjectInput = document.getElementById('subject')
        const highPriorityInput= document.getElementById('priority')
        const contentInput= document.getElementById('content')

        //fetch the template content
        const url = `/${restUrl}/template/${templateId}`
        const response = await fetch(url, {
            headers: {
                "Content-Type": "application/json",
                'X-CSRF-TOKEN': token
            },
        })

        if (!response) {
            console.error("Server responded with error", response.statusText)
            errorHandler("Fejl! Kunne ikke hente skabelonens indhold")
        }

        //fill input fields with template
        const responseTemplateObject = await response.json()

        subjectInput.value = responseTemplateObject.subject
        highPriorityInput.value = responseTemplateObject.highPriority
        contentInput.value = responseTemplateObject.content
    }
}