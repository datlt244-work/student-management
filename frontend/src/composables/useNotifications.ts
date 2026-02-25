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
                const token = await getToken(messaging, {
                    vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY
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
        onMessage(messaging, (payload: any) => {
            console.log('Message received. ', payload);
            // Hiển thị thông báo (Toast/Alert)
            if (payload.notification) {
                alert(`${payload.notification.title}: ${payload.notification.body}`);
            }
        });
    };

    return {
        requestPermission,
        listenForMessages
    };
}
