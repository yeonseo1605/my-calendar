// Direct user-clicked Android intent; use the host supported by older APKs too.
(() => {
  const android = /Android/i.test(navigator.userAgent);
  const fallback = new URL('./widget-open.html', location.href).href;
  const intent = 'intent://widget-settings#Intent;scheme=mycalendar;'
    + 'package=com.yeonseo.mycalendar;component=com.yeonseo.mycalendar/.MainActivity;'
    + 'S.browser_fallback_url=' + encodeURIComponent(fallback) + ';end';
  document.querySelectorAll('[data-widget-open]').forEach(link => {
    link.href = android ? intent : fallback;
    link.addEventListener('click', () => {
      const help = document.getElementById('widgetOpenHelp');
      if (help) help.hidden = false;
    });
  });
})();
