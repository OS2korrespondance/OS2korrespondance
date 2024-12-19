
function errorHandler(fallbackMessage) {
    return function (response) {
        if (response.responseText !== null && response.responseText !== undefined && response.responseText.startsWith("{")) {
            let responseObj = JSON.parse(response.responseText);
            toastr.warning(responseObj.error);
            return;
        }
        if (response.responseText != null && response.responseText !== "") {
            toastr.warning(response.responseText);
        } else if (response.status === 403) {
            toastr.warning("Adgang nægtet: Enten er din session udløbet, eller du har ikke de nødvendige rettigheder til at tilgå denne funktion.");
        } else {
            toastr.error(fallbackMessage);
        }
    }
}

let defaultErrorHandler = errorHandler('Ukendt fejl');
