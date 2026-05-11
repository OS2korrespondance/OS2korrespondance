$(document).ready(function() {
    mailService = new MailService();
    mailService.init();
    templateService = new TemplateService()

    //Init cpr lookup function (element only exists when composing new mail, not when answering)
    const cprInput = document.getElementById('patientCpr')
    if (cprInput) {
        cprInput.addEventListener('input', (event)=> mailService.onCprChange(event))
        cprInput.dispatchEvent(new Event("input"))
    }

    const messageTemplateSelect = document.getElementById('messageTemplateSelect')
    if (messageTemplateSelect) {
        messageTemplateSelect.addEventListener('change', (event)=> templateService.onTemplateSelectionChange(messageTemplateSelect))
    }

    // Preload attachments
    if (typeof preloadedFiles !== 'undefined' && preloadedFiles && preloadedFiles.length > 0) {
        preloadedFiles.forEach(function(file) {
            mailService.preLoadAttachment(file.fileName, file.content, file.size);
        });
    }

    // Append email signature to content field
    if (typeof emailSignature !== 'undefined' && emailSignature && emailSignature.trim().length > 0) {
        const contentField = document.getElementById('content');
        const existingContent = contentField.value.trim();
        if (existingContent.length === 0) {
            // New mail or answer: signature only
            contentField.value = "\n\n" + emailSignature;
        } else {
            // Forwarded mail: signature between new area and forwarded content
            contentField.value = "\n\n" + emailSignature + "\n\n" + existingContent;
        }
        // Update character counter
        var characterCount = contentField.value.length;
        $("#counter").text(characterCount);
    }
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

    this.preLoadAttachment = function(fileName, content, size) {
        $.ajax({
            url: "/rest/mails/" + newMailId + "/attachment/add-preloaded",
            type: 'POST',
            headers: {
                'X-CSRF-TOKEN': token,
                'Content-Type': 'application/json'
            },
            data: JSON.stringify({
                fileName: fileName,
                content: content
            }),
            success: function(attachmentId, textStatus, jQxhr) {
                if (attachmentId != '') {
                    var ulElement = $('#attachments');
                    var txtHtml  = '<li id="attachment-' + attachmentId + '">\n';
                    txtHtml += '<a style="color: black;" title="Slet vedhæftning" onclick="mailService.removeAttachment(' + attachmentId + ');"><em class="fa fa-fw fa-remove"></em></a>\n';
                    txtHtml += '<span>' + fileName + '</span>\n';
                    txtHtml += '</li>';

                    ulElement.append(txtHtml);
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                defaultErrorHandler(jqXHR, textStatus, errorThrown);
            }
        });
    }

    this.send = function() {
        var sendButton = $("#sendButton");
        sendButton.prop("disabled", true);
        sendButton.html('<i class="fa fa-spinner fa-spin"></i> Sender...');

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
                    toastr.success("Beskeden er sendt! ");
                    setTimeout(() => {window.location.href="/receipt/" + data}, 5000);
                }

            },
            error: function(jqXHR, textStatus, errorThrown) {
                sendButton.prop("disabled", false);
                sendButton.html('<i class="fa fa-reply"></i> Send');
                defaultErrorHandler(jqXHR, textStatus, errorThrown);
            }
        });
    }

    this.onCprChange = async (event) => {
        const cprValue = event.target.value
        const hint = document.getElementById('gpHint')
        if (!cprValue || cprValue.length !== 10) {
            if (hint) hint.textContent = ''
            return
        }

        const url = cprLookupUrl + cprValue + '/extended'
        const response = await fetch(url, {
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': token
            }
        })

        if (!response.ok) {
            console.error('CPR/SF0830 lookup failed', response.status, response.statusText)
            return
        }

        const data = await response.json()

        const nameInput = document.getElementById('patientName')
        if (nameInput && data.patientName) {
            nameInput.value = data.patientName
        }

        const recipientSelect = document.getElementById('recipient')
        if (data.matchedRecipientEan && recipientSelect) {
            const option = Array.from(recipientSelect.options).find(o => o.value === data.matchedRecipientEan)
            if (option) {
                option.selected = true
                recipientSelect.dispatchEvent(new Event('change'))
                if (hint) hint.textContent = ''
                return
            }
        }

        if (recipientSelect && recipientSelect.value) {
            recipientSelect.value = ''
            recipientSelect.dispatchEvent(new Event('change'))
        }

        if (hint) {
            if (data.doctorPraksisbetegnelse) {
                hint.textContent = 'Borgerens læge: ' + data.doctorPraksisbetegnelse +
                    (data.doctorYdernummer ? ' (ydernr. ' + data.doctorYdernummer + ')' : '') +
                    ' – ikke i modtagerlisten'
            } else {
                hint.textContent = 'Ingen læge fundet for denne borger'
            }
        }
    }
}

/**
 * Appends the user's email signature to the given content string.
 * Returns the content unchanged if no signature is configured.
 */
function appendSignature(content) {
    if (typeof emailSignature === 'undefined' || !emailSignature || emailSignature.trim().length === 0) {
        return content;
    }
    const trimmed = content ? content.trim() : "";
    if (trimmed.length === 0) {
        return "\n\n" + emailSignature;
    }
    return trimmed + "\n\n" + emailSignature;
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

        //fill input fields with template content, then re-append signature
        const responseTemplateObject = await response.json()

        subjectInput.value = responseTemplateObject.subject
        highPriorityInput.value = responseTemplateObject.highPriority
        contentInput.value = appendSignature(responseTemplateObject.content)

        // Update character counter after template + signature insertion
        $("#counter").text(contentInput.value.length);
    }
}