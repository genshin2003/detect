import { defineStore } from 'pinia';
import request from '/@/utils/request';

export const useMessageStore = defineStore('message', {
	state: () => ({
		unreadCount: 0,
	}),
	actions: {
		async fetchUnreadCount() {
			try {
				const res = await request.get('/api/message/unreadCount');
				if (res.code == 0) {
					this.unreadCount = res.data.count || 0;
				}
			} catch (e) {
				console.error('获取未读数量失败:', e);
			}
		},
		async markAllAsRead() {
			try {
				await request.post('/api/message/readAll');
				this.unreadCount = 0;
			} catch (e) {
				console.error('全部标记已读失败:', e);
			}
		},
	},
});
