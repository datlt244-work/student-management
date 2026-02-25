// public/firebase-messaging-sw.js
importScripts('https://www.gstatic.com/firebasejs/10.13.2/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.13.2/firebase-messaging-compat.js');

self.addEventListener('install', () => {
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil(clients.claim());
});

firebase.initializeApp({
    apiKey: "AIzaSyBajkhjil6XdhrSR-2HSXOCpEiEdTjHUYk",
    authDomain: "student-management-24137.firebaseapp.com",
    projectId: "student-management-24137",
    storageBucket: "student-management-24137.firebasestorage.app",
    messagingSenderId: "737453803180",
    appId: "1:737453803180:web:7923873ef26cfaed724109"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
    const notificationTitle = payload.notification.title;
    const notificationOptions = {
        body: payload.notification.body,
        icon: '/favicon.ico',
        data: {
            url: payload.data?.url || '/'
        }
    };
    self.registration.showNotification(notificationTitle, notificationOptions);
});

self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    const urlToOpen = event.notification.data?.url || '/';
    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
            // Nếu đã mở trang đó rồi thì tập trung vào nó, không thì mở tab mới
            for (let i = 0; i < windowClients.length; i++) {
                const client = windowClients[i];
                if (client.url === urlToOpen && 'focus' in client) {
                    return client.focus();
                }
            }
            if (clients.openWindow) {
                return clients.openWindow(urlToOpen);
            }
        })
    );
});