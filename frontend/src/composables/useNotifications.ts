import { getToken, onMessage, messaging } from '@/firebase';
import { apiFetch } from '@/utils/api';

export function useNotifications() {
    const requestPermission = async () => {
        if (!('Notification' in window)) {
            console.log('This browser does not support desktop notification');
            return;
        }

        try {
            const permission = await Notification.requestPermission();
            if (permission === 'granted') {

                // Register Service Worker explicitly to avoid 401 issues
                const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js', {
                    scope: '/'
                });
                
                // Ensure the service worker is active before subscribing
                await navigator.serviceWorker.ready;
                
                const token = await getToken(messaging, {
                    vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY,
                    serviceWorkerRegistration: registration
                });

                if (token) {
                    console.log('FCM Token registered successfully');
                    // Gửi token lên server dùng apiFetch để có Auth Header (nếu đã login)
                    await apiFetch('/notifications/tokens', {
                        method: 'POST',
                        body: JSON.stringify({
                            token: token,
                            deviceType: 'web'
                        })
                    }).catch(err => {
                        console.warn('Could not register token on server. User might not be logged in.', err);
                    });
                }
            }
        } catch (error) {
            console.error('Error getting notification permission:', error);
        }
    };

    const listenForMessages = () => {
        onMessage(messaging, (payload) => {
            console.log('Message received. ', payload);
            // Hiển thị thông báo (Toast/Alert)
            const notification = payload.notification;
            if (notification) {
                alert(`${notification.title}: ${notification.body}`);
            }
        });
    };

    return {
        requestPermission,
        listenForMessages
    };
}
