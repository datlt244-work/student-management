// public/firebase-messaging-sw.js
importScripts('https://www.gstatic.com/firebasejs/10.8.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.8.0/firebase-messaging-compat.js');

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
        icon: '/favicon.ico'
    };
    self.registration.showNotification(notificationTitle, notificationOptions);
});