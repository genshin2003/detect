<template>
	<div class="system-role-container layout-padding">
		<div class="system-role-padding layout-padding-auto layout-padding-view">
			<div class="system-user-search mb15">
				<el-select v-model="state.tableData.param.type" placeholder="消息类型" clearable style="max-width: 150px">
					<el-option label="检测通知" value="DETECT" />
					<el-option label="批量检测" value="BATCH_DETECT" />
					<el-option label="系统公告" value="ANNOUNCE" />
					<el-option label="安全提醒" value="SECURITY" />
				</el-select>
				<el-select v-model="state.tableData.param.isRead" placeholder="已读状态" clearable style="max-width: 130px; margin-left: 10px">
					<el-option label="未读" :value="false" />
					<el-option label="已读" :value="true" />
				</el-select>
				<el-button size="default" type="primary" class="predict-button" @click="getTableData()">
					<el-icon><ele-Search /></el-icon>
					查询
				</el-button>
				<el-button size="default" @click="resetSearch" style="margin-left: 10px">重置</el-button>
				<el-button size="default" type="success" style="margin-left: 10px" @click="onMarkAllRead">全部已读</el-button>
				<el-button size="default" type="danger" style="margin-left: 10px" :disabled="selectedIds.length === 0" @click="onBatchDelete">批量删除</el-button>
			</div>

			<el-table :data="state.tableData.data" style="width: 100%" :row-class-name="tableRowClassName" :header-cell-style="{ background: '#f5f7fa' }" @selection-change="onSelectionChange">
				<el-table-column type="selection" width="55" align="center" />
				<el-table-column prop="num" label="序号" width="80" align="center" />
				<el-table-column prop="title" label="标题" show-overflow-tooltip align="center" />
				<el-table-column prop="content" label="内容" show-overflow-tooltip align="center" />
				<el-table-column prop="type" label="类型" width="120" align="center">
					<template #default="scope">
						<el-tag :type="getTypeTag(scope.row.type)">{{ getTypeLabel(scope.row.type) }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="接收者" width="120" align="center">
					<template #default="scope">
						<el-tag v-if="scope.row.userId === 0" type="warning" size="small">全体用户</el-tag>
						<el-tag v-else type="info" size="small">{{ scope.row.receiverName || '未知' }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column prop="priority" label="优先级" width="100" align="center">
					<template #default="scope">
						<el-tag :type="scope.row.priority === 'HIGH' ? 'danger' : 'info'">
							{{ scope.row.priority === 'HIGH' ? '紧急' : '普通' }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column prop="isRead" label="状态" width="80" align="center">
					<template #default="scope">
						<el-tag :type="scope.row.isRead ? 'success' : 'warning'">
							{{ scope.row.isRead ? '已读' : '未读' }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column prop="createTime" label="时间" width="180" align="center" />
				<el-table-column label="操作" width="160" align="center">
					<template #default="scope">
						<el-button size="small" text type="primary" @click="onViewDetail(scope.row)">查看</el-button>
						<el-button size="small" text type="danger" @click="onDelete(scope.row)">删除</el-button>
					</template>
				</el-table-column>
			</el-table>

			<el-pagination @size-change="onHandleSizeChange" @current-change="onHandleCurrentChange" class="mt15"
				:pager-count="5" :page-sizes="[10, 20, 30]" v-model:current-page="state.tableData.param.pageNum"
				background v-model:page-size="state.tableData.param.pageSize"
				layout="total, sizes, prev, pager, next, jumper" :total="state.tableData.total">
			</el-pagination>

			<!-- 消息详情对话框 -->
			<el-dialog v-model="detailVisible" title="消息详情" width="500px">
				<div v-if="currentMessage">
					<div style="margin-bottom: 15px">
						<el-tag :type="getTypeTag(currentMessage.type)">{{ getTypeLabel(currentMessage.type) }}</el-tag>
						<el-tag v-if="currentMessage.priority === 'HIGH'" type="danger" effect="dark" style="margin-left: 8px">
								<el-icon><ele-WarnTriangleFilled /></el-icon> 紧急公告
							</el-tag>
						<el-tag v-if="currentMessage.userId === 0" type="warning" style="margin-left: 8px">全体用户</el-tag>
						<el-tag v-else type="info" style="margin-left: 8px">{{ currentMessage.receiverName }}</el-tag>
					</div>
					<h3 :style="{ marginBottom: '10px', color: currentMessage.priority === 'HIGH' ? '#f56c6c' : '#333' }">
						{{ currentMessage.priority === 'HIGH' ? '【紧急】' : '' }}{{ currentMessage.title }}
					</h3>
					<el-divider />
					<p style="line-height: 1.8; color: #333; white-space: pre-wrap;">{{ currentMessage.content }}</p>
					<el-divider />
					<p style="text-align: right; color: #999">{{ currentMessage.createTime }}</p>
				</div>
			</el-dialog>
		</div>
	</div>
</template>

<script setup lang="ts" name="messageCenter">
import { reactive, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '/@/utils/request';
import { useMessageStore } from '/@/stores/message';
import { useUserInfo } from '/@/stores/userInfo';

const messageStore = useMessageStore();
const userInfoStore = useUserInfo();
const detailVisible = ref(false);
const currentMessage = ref<any>(null);
const selectedIds = ref<number[]>([]);

const isAdmin = ref(false);

const state = reactive({
	tableData: {
		data: [] as any[],
		total: 0,
		loading: false,
		param: {
			type: '',
			isRead: null as boolean | null,
			pageNum: 1,
			pageSize: 10,
		},
	},
});

const getTypeLabel = (type: string) => {
	const map: Record<string, string> = {
		DETECT: '检测通知',
		BATCH_DETECT: '批量检测',
		ANNOUNCE: '系统公告',
		SECURITY: '安全提醒',
	};
	return map[type] || type;
};

const getTypeTag = (type: string) => {
	const map: Record<string, string> = {
		DETECT: 'primary',
		BATCH_DETECT: 'success',
		ANNOUNCE: 'warning',
		SECURITY: 'danger',
	};
	return map[type] || 'info';
};

const tableRowClassName = ({ row }: { row: any }) => {
	if (row.priority === 'HIGH') return 'urgent-row';
	if (!row.isRead) return 'unread-row';
	return '';
};

const getTableData = () => {
	state.tableData.loading = true;
	const params: any = {
		pageNum: state.tableData.param.pageNum,
		pageSize: state.tableData.param.pageSize,
	};
	if (state.tableData.param.type) params.type = state.tableData.param.type;
	if (state.tableData.param.isRead !== null) params.isRead = state.tableData.param.isRead;

	request.get('/api/message/list', { params }).then((res) => {
		if (res.code == 0) {
			state.tableData.data = res.data.records.map((item: any, index: number) => ({
				...item,
				num: index + 1 + (state.tableData.param.pageNum - 1) * state.tableData.param.pageSize,
			}));
			state.tableData.total = res.data.total;
		} else {
			ElMessage({ type: 'error', message: res.msg || '请求失败' });
		}
		state.tableData.loading = false;
	});
};

const resetSearch = () => {
	state.tableData.param.type = '';
	state.tableData.param.isRead = null;
	getTableData();
};

const onViewDetail = (row: any) => {
	currentMessage.value = row;
	detailVisible.value = true;
	if (!row.isRead) {
		request.post(`/api/message/read/${row.id}`).then(() => {
			row.isRead = true;
			messageStore.fetchUnreadCount();
		});
	}
};

const onMarkAllRead = () => {
	messageStore.markAllAsRead().then(() => {
		ElMessage({ type: 'success', message: '全部标记已读' });
		getTableData();
	});
};

const onSelectionChange = (selection: any[]) => {
	selectedIds.value = selection.map((item: any) => item.id);
};

const onDelete = (row: any) => {
	ElMessageBox.confirm('确定要删除这条消息吗？', '提示', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	}).then(() => {
		request.delete(`/api/message/${row.id}`).then((res) => {
			if (res.code == 0) {
				ElMessage({ type: 'success', message: '删除成功' });
				messageStore.fetchUnreadCount();
				getTableData();
			} else {
				ElMessage({ type: 'error', message: res.msg || '删除失败' });
			}
		});
	}).catch(() => {});
};

const onBatchDelete = () => {
	if (selectedIds.value.length === 0) {
		ElMessage({ type: 'warning', message: '请先选择要删除的消息' });
		return;
	}
	ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条消息吗？`, '提示', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	}).then(() => {
		request.delete('/api/message/batch', { data: selectedIds.value }).then((res) => {
			if (res.code == 0) {
				ElMessage({ type: 'success', message: '批量删除成功' });
				messageStore.fetchUnreadCount();
				getTableData();
			} else {
				ElMessage({ type: 'error', message: res.msg || '批量删除失败' });
			}
		});
	}).catch(() => {});
};

const onHandleSizeChange = (val: number) => {
	state.tableData.param.pageSize = val;
	getTableData();
};

const onHandleCurrentChange = (val: number) => {
	state.tableData.param.pageNum = val;
	getTableData();
};

onMounted(() => {
	// 检查是否是管理员
	isAdmin.value = userInfoStore.userInfos.roles.includes('admin');
	getTableData();
});
</script>

<style scoped lang="scss">
.predict-button {
	margin-left: 10px;
	background: #4CAF50;
	border-color: #4CAF50;
	&:hover {
		background: #2E7D32;
		border-color: #2E7D32;
	}
}
.system-role-container {
	.system-role-padding {
		padding: 15px;
		.el-table {
			flex: 1;
		}
	}
}
:deep(.el-table .unread-row) {
	background-color: #f0f9ff;
}

:deep(.el-table .urgent-row) {
	background-color: #fef0f0;
	border-left: 3px solid #f56c6c;
	font-weight: 500;
}
</style>
