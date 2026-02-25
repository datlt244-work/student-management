import { onMounted } from 'vue';
import { useNotifications } from './useNotifications';

export function useNotificationSetup() {
    const { requestPermission, listenForMessages } = useNotifications();

    onMounted(() => {
        requestPermission();
        listenForMessages();
    });
}
