import { getToken, onMessage, messaging } from '@/firebase';
import { apiFetch } from '@/utils/api';

export function useNotifications() {
    const requestPermission = async () => {
        try {
            const permission = await Notification.requestPermission();
            if (permission === 'granted') {
                const token = await getToken(messaging, {
                    vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY
                });

                if (token) {
                    console.log('FCM Token:', token);
                    // Gửi token lên server dùng apiFetch để có Auth Header
                    await apiFetch('/notifications/tokens', {
                        method: 'POST',
                        body: JSON.stringify({
                            token: token,
                            deviceType: 'web'
                        })
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
