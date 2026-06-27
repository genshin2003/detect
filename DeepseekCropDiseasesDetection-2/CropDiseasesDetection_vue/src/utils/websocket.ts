import { Session } from '/@/utils/storage';
import { ElNotification } from 'element-plus';
import { h } from 'vue';
import { useMessageStore } from '/@/stores/message';

class MessageSocket {
	private socket: WebSocket | null = null;
	private reconnectTimer: any = null;
	private onMessageCallback: ((data: any) => void) | null = null;

	connect() {
		const token = Session.get('token');
		if (!token) return;

		const wsUrl = `ws://localhost:9999/ws/message?token=${token}`;
		this.socket = new WebSocket(wsUrl);

		this.socket.onopen = () => {
			console.log('WebSocket连接建立');
			if (this.reconnectTimer) {
				clearTimeout(this.reconnectTimer);
				this.reconnectTimer = null;
			}
		};

		this.socket.onmessage = (event) => {
			try {
				const data = JSON.parse(event.data);

				if (data.type === 'notification') {
					// 更新未读数量
					const messageStore = useMessageStore();
					messageStore.fetchUnreadCount();

					// 根据消息类型和优先级设置通知样式
					let notificationType: 'success' | 'warning' | 'info' | 'error' = 'info';
					let duration = 5000;

					if (data.messageType === 'SECURITY') {
						notificationType = 'error';
						duration = 10000;
					} else if (data.priority === 'HIGH') {
						notificationType = 'error';
						duration = 15000;
					} else if (data.messageType === 'BATCH_DETECT') {
						notificationType = 'success';
						duration = 6000;
					} else if (data.messageType === 'DETECT') {
						notificationType = 'success';
						duration = 5000;
					} else {
						notificationType = 'info';
						duration = 5000;
					}

					// 紧急公告使用特殊样式
					if (data.priority === 'HIGH') {
						ElNotification({
							title: '【紧急公告】' + (data.title || '新消息'),
							message: h('div', {
								style: 'color: #f56c6c; font-weight: 500; line-height: 1.6;'
							}, data.content || ''),
							type: 'error',
							duration: 15000,
							position: 'top-right',
							showClose: true,
							dangerouslyUseHTMLString: true,
							customClass: 'urgent-notification',
						});
					} else {
						// 普通消息通知
						ElNotification({
							title: data.title || '新消息',
							message: data.content || '',
							type: notificationType,
							duration: duration,
							position: 'top-right',
						});
					}
				}

				if (data.type === 'unreadCount') {
					// 直接更新未读数量
					const messageStore = useMessageStore();
					messageStore.unreadCount = data.count || 0;
				}

				if (this.onMessageCallback) {
					this.onMessageCallback(data);
				}
			} catch (e) {
				console.error('解析WebSocket消息失败:', e);
			}
		};

		this.socket.onclose = () => {
			console.log('WebSocket连接关闭，5秒后重连');
			// 只有在有token的情况下才重连
			const currentToken = Session.get('token');
			if (currentToken) {
				this.reconnectTimer = setTimeout(() => this.connect(), 5000);
			}
		};

		this.socket.onerror = (error) => {
			console.error('WebSocket错误:', error);
		};
	}

	disconnect() {
		if (this.reconnectTimer) {
			clearTimeout(this.reconnectTimer);
			this.reconnectTimer = null;
		}
		if (this.socket) {
			this.socket.close();
			this.socket = null;
		}
	}

	onMessage(callback: (data: any) => void) {
		this.onMessageCallback = callback;
	}

	// 发送标记已读消息
	markRead(messageId: number) {
		if (this.socket && this.socket.readyState === WebSocket.OPEN) {
			this.socket.send(JSON.stringify({
				type: 'markRead',
				messageId: messageId
			}));
		}
	}
}

export const messageSocket = new MessageSocket();
