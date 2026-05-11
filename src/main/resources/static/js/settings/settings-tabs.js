// settings-tabs.js
// Shared tab initialization and lazy-loading for the settings page.
// Depends on: jQuery, Bootstrap tabs, iCheck, and the global `token` CSRF variable.

var settingsTabsLoaded = {};

function initSettingsTabs() {
    $('#settingsTabs a[data-toggle="tab"]').on('shown.bs.tab', function () {
        var tabId = this.id;
        if (!settingsTabsLoaded[tabId]) {
            loadTabContent(this);
        }
    });

    // Load the initially active tab
    var activeTab = document.querySelector('#settingsTabs .nav-link.active');
    if (activeTab) {
        loadTabContent(activeTab);
    }
}

function loadTabContent(tabElement) {
    var loaderName = tabElement.dataset.loader;
    if (loaderName && typeof window[loaderName] === 'function') {
        window[loaderName]();
        settingsTabsLoaded[tabElement.id] = true;
    }
}

function initIChecks() {
    $('.i-checks').iCheck({
        checkboxClass: 'icheckbox_square-green'
    });
}