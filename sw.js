const CACHE='my-calendar-v239';
const FILES=['./','./index.html','./styles.css?v=170','./app.js?v=168','./cloud-store.js?v=4','./manifest.webmanifest','./assets/favicon-64.png','./assets/apple-touch-icon-180.png','./assets/icon-192.png','./assets/icon-512.png','./assets/my-calendar-share-1200x630.png','./assets/brand-calendar-transparent.png'];
self.addEventListener('install',event=>event.waitUntil(Promise.all([
  caches.open(CACHE).then(cache=>cache.addAll(FILES)),
  self.skipWaiting()
])));
self.addEventListener('activate',event=>event.waitUntil(Promise.all([
  caches.keys().then(keys=>Promise.all(keys.filter(key=>key!==CACHE).map(key=>caches.delete(key)))),
  self.clients.claim()
])));
self.addEventListener('fetch',event=>{if(event.request.method==='GET')event.respondWith(fetch(event.request).then(response=>{const copy=response.clone();caches.open(CACHE).then(cache=>cache.put(event.request,copy));return response}).catch(()=>caches.match(event.request)))});
