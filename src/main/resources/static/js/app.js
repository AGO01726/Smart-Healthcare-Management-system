/* Smart Healthcare Management System - small, dependency-free UI helpers (CSP friendly: no inline scripts). */
(function () {
  'use strict';

  // Confirm dialogs: <form data-confirm="Are you sure?">
  document.querySelectorAll('form[data-confirm]').forEach(function (form) {
    form.addEventListener('submit', function (event) {
      if (!window.confirm(form.getAttribute('data-confirm'))) {
        event.preventDefault();
      }
    });
  });

  // Mobile sidebar toggle
  var sidebar = document.getElementById('sidebar');
  var backdrop = document.getElementById('sidebarBackdrop');
  var toggle = document.getElementById('sidebarToggle');
  function setSidebar(open) {
    if (!sidebar) { return; }
    sidebar.classList.toggle('open', open);
    if (backdrop) { backdrop.classList.toggle('show', open); }
  }
  if (toggle) { toggle.addEventListener('click', function () { setSidebar(!sidebar.classList.contains('open')); }); }
  if (backdrop) { backdrop.addEventListener('click', function () { setSidebar(false); }); }

  // Auto-dismiss success alerts after 6 seconds
  document.querySelectorAll('.alert-success.auto-dismiss').forEach(function (alertEl) {
    setTimeout(function () {
      alertEl.classList.remove('show');
      setTimeout(function () { alertEl.remove(); }, 300);
    }, 6000);
  });

  // Client-side filter for small tables: <input data-table-filter="tableId">
  document.querySelectorAll('input[data-table-filter]').forEach(function (input) {
    var table = document.getElementById(input.getAttribute('data-table-filter'));
    if (!table) { return; }
    input.addEventListener('input', function () {
      var term = input.value.trim().toLowerCase();
      table.querySelectorAll('tbody tr').forEach(function (row) {
        row.style.display = row.textContent.toLowerCase().indexOf(term) === -1 ? 'none' : '';
      });
    });
  });

  // Appointment slot picker: shows free slots for the chosen doctor + date and fills the time input.
  var slotBox = document.getElementById('slotBox');
  var dateInput = document.getElementById('appointmentDate');
  var timeInput = document.getElementById('startTime');
  if (slotBox && dateInput && timeInput) {
    var doctorSelect = document.getElementById('doctorId');
    var fixedDoctor = slotBox.getAttribute('data-doctor-id');
    var excludeId = slotBox.getAttribute('data-exclude-id');

    function showMessage(text) {
      slotBox.textContent = '';
      var span = document.createElement('span');
      span.className = 'text-muted small';
      span.textContent = text;
      slotBox.appendChild(span);
    }

    function loadSlots() {
      var doctorId = doctorSelect ? doctorSelect.value : fixedDoctor;
      var date = dateInput.value;
      if (!doctorId || !date) { showMessage('Select a doctor and a date to see free slots.'); return; }
      var url = '/appointments/slots?doctorId=' + encodeURIComponent(doctorId) + '&date=' + encodeURIComponent(date);
      if (excludeId) { url += '&excludeId=' + encodeURIComponent(excludeId); }
      showMessage('Loading free slots...');
      fetch(url, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
        .then(function (response) { if (!response.ok) { throw new Error('HTTP ' + response.status); } return response.json(); })
        .then(function (slots) {
          slotBox.textContent = '';
          if (!slots.length) { showMessage('No free slots on this date (doctor unavailable or fully booked).'); return; }
          slots.forEach(function (slot) {
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'btn btn-sm slot-btn me-2 mb-2 ' + (timeInput.value === slot ? 'btn-primary' : 'btn-outline-primary');
            button.textContent = slot;
            button.addEventListener('click', function () {
              timeInput.value = slot;
              slotBox.querySelectorAll('button').forEach(function (b) { b.className = 'btn btn-sm slot-btn me-2 mb-2 btn-outline-primary'; });
              button.className = 'btn btn-sm slot-btn me-2 mb-2 btn-primary';
            });
            slotBox.appendChild(button);
          });
        })
        .catch(function () { showMessage('Could not load slots. You can still type a time manually.'); });
    }

    if (doctorSelect) { doctorSelect.addEventListener('change', loadSlots); }
    dateInput.addEventListener('change', loadSlots);
    loadSlots();
  }
})();
