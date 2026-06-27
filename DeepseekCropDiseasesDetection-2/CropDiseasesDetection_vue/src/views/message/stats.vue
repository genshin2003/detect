<template>
	<div class="message-stats-container layout-padding">
		<div class="stats-content layout-padding-auto layout-padding-view">
			<el-row :gutter="20" class="stats-row">
				<!-- 消息总数卡片 -->
				<el-col :span="6">
					<el-card class="stats-card total-card" shadow="hover">
						<div class="card-content">
							<div class="card-icon">
								<el-icon :size="48" color="#409EFF"><ele-Bell /></el-icon>
							</div>
							<div class="card-info">
								<div class="card-value">{{ stats.totalCount || 0 }}</div>
								<div class="card-label">消息总数</div>
							</div>
						</div>
					</el-card>
				</el-col>

				<!-- 管理员未读卡片 -->
				<el-col :span="6">
					<el-card class="stats-card unread-card" shadow="hover">
						<div class="card-content">
							<div class="card-icon">
								<el-icon :size="48" color="#E6A23C"><ele-Message /></el-icon>
							</div>
							<div class="card-info">
								<div class="card-value">{{ stats.adminUnreadCount || 0 }}</div>
								<div class="card-label">管理员未读</div>
							</div>
						</div>
					</el-card>
				</el-col>

				<!-- 公告数量卡片 -->
				<el-col :span="6">
					<el-card class="stats-card announce-card" shadow="hover">
						<div class="card-content">
							<div class="card-icon">
								<el-icon :size="48" color="#67C23A"><ele-Notification /></el-icon>
							</div>
							<div class="card-info">
								<div class="card-value">{{ stats.broadcastStats?.announceCount || 0 }}</div>
								<div class="card-label">系统公告数</div>
							</div>
						</div>
					</el-card>
				</el-col>

				<!-- 公告阅读率卡片 -->
				<el-col :span="6">
					<el-card class="stats-card rate-card" shadow="hover">
						<div class="card-content">
							<div class="card-icon">
								<el-icon :size="48" color="#F56C6C"><ele-DataAnalysis /></el-icon>
							</div>
							<div class="card-info">
								<div class="card-value">{{ stats.broadcastStats?.readRate || 0 }}%</div>
								<div class="card-label">公告阅读率</div>
							</div>
						</div>
					</el-card>
				</el-col>
			</el-row>

			<el-row :gutter="20" class="stats-row">
				<!-- 各类型消息分布 -->
				<el-col :span="12">
					<el-card class="chart-card" shadow="hover">
						<template #header>
							<span class="chart-title">消息类型分布</span>
						</template>
						<div class="chart-container">
							<div class="type-list">
								<div class="type-item" v-for="(item, index) in typeList" :key="index">
									<div class="type-info">
										<el-tag :type="item.tagType" size="small">{{ item.label }}</el-tag>
										<span class="type-count">{{ item.count }} 条</span>
									</div>
									<div class="type-bar">
										<div class="type-bar-inner" :style="{ width: item.percentage + '%', backgroundColor: item.color }"></div>
									</div>
									<span class="type-percentage">{{ item.percentage }}%</span>
								</div>
							</div>
						</div>
					</el-card>
				</el-col>

				<!-- 公告阅读情况详情 -->
				<el-col :span="12">
					<el-card class="chart-card" shadow="hover">
						<template #header>
							<div class="chart-header">
								<span class="chart-title">公告阅读详情</span>
								<el-tag type="info" size="small">按用户×公告数计算</el-tag>
							</div>
						</template>
						<div class="chart-container">
							<div class="announce-stats">
								<div class="announce-item">
									<div class="announce-label">系统公告总数</div>
									<div class="announce-value">{{ stats.broadcastStats?.announceCount || 0 }}</div>
								</div>
								<div class="announce-item">
									<div class="announce-label">总用户数</div>
									<div class="announce-value">{{ userCount }}</div>
								</div>
								<div class="announce-item">
									<div class="announce-label">已读记录数</div>
									<div class="announce-value read">{{ stats.broadcastStats?.totalReadRecords || 0 }}</div>
								</div>
								<div class="announce-item">
									<div class="announce-label">应读记录数</div>
									<div class="announce-value">{{ shouldReadCount }}</div>
								</div>
								<div class="announce-progress">
									<el-progress
										:percentage="stats.broadcastStats?.readRate || 0"
										:stroke-width="20"
										:format="(percentage: number) => percentage + '%'"
										:color="progressColors"
									/>
									<span class="progress-label">公告阅读率（已读/应读）</span>
								</div>
							</div>
						</div>
					</el-card>
				</el-col>
			</el-row>

			<el-row :gutter="20" class="stats-row">
				<!-- 消息统计概览 -->
				<el-col :span="24">
					<el-card class="chart-card" shadow="hover">
						<template #header>
							<div class="chart-header">
								<span class="chart-title">消息统计概览</span>
								<el-button type="primary" size="small" @click="refreshStats">
									<el-icon><ele-Refresh /></el-icon>
									刷新数据
								</el-button>
							</div>
						</template>
						<div class="overview-container">
							<div class="overview-item" v-for="(item, index) in overviewList" :key="index">
								<div class="overview-icon" :style="{ backgroundColor: item.bgColor }">
									<el-icon :size="24" :color="item.iconColor"><component :is="item.icon" /></el-icon>
								</div>
								<div class="overview-info">
									<div class="overview-value">{{ item.value }}</div>
									<div class="overview-label">{{ item.label }}</div>
								</div>
							</div>
						</div>
					</el-card>
				</el-col>
			</el-row>
		</div>
	</div>
</template>

<script setup lang="ts" name="messageStats">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import request from '/@/utils/request';

const stats = ref<any>({});
const userCount = ref(0);

const progressColors = [
	{ color: '#F56C6C', percentage: 30 },
	{ color: '#E6A23C', percentage: 60 },
	{ color: '#67C23A', percentage: 100 },
];

// 应读记录数 = 公告数 × 用户数
const shouldReadCount = computed(() => {
	return (stats.value.broadcastStats?.announceCount || 0) * userCount.value;
});

const typeList = computed(() => {
	const totalCount = stats.value.totalCount || 1;
	const typeCount = stats.value.typeCount || {};
	const typePercentage = stats.value.typePercentage || {};

	return [
		{
			label: '系统公告',
			count: typeCount.ANNOUNCE || 0,
			percentage: (typePercentage.ANNOUNCE || 0).toFixed(1),
			tagType: 'warning' as const,
			color: '#E6A23C',
		},
		{
			label: '安全警告',
			count: typeCount.SECURITY || 0,
			percentage: (typePercentage.SECURITY || 0).toFixed(1),
			tagType: 'danger' as const,
			color: '#F56C6C',
		},
		{
			label: '检测通知',
			count: typeCount.DETECT || 0,
			percentage: (typePercentage.DETECT || 0).toFixed(1),
			tagType: 'primary' as const,
			color: '#409EFF',
		},
		{
			label: '批量检测',
			count: typeCount.BATCH_DETECT || 0,
			percentage: (typePercentage.BATCH_DETECT || 0).toFixed(1),
			tagType: 'success' as const,
			color: '#67C23A',
		},
	];
});

const overviewList = computed(() => [
	{
		label: '消息总数',
		value: stats.value.totalCount || 0,
		icon: 'ele-Bell',
		bgColor: '#ECF5FF',
		iconColor: '#409EFF',
	},
	{
		label: '管理员未读',
		value: stats.value.adminUnreadCount || 0,
		icon: 'ele-Message',
		bgColor: '#FDF6EC',
		iconColor: '#E6A23C',
	},
	{
		label: '系统公告',
		value: stats.value.typeCount?.ANNOUNCE || 0,
		icon: 'ele-Notification',
		bgColor: '#FDF6EC',
		iconColor: '#E6A23C',
	},
	{
		label: '安全警告',
		value: stats.value.typeCount?.SECURITY || 0,
		icon: 'ele-WarnTriangleFilled',
		bgColor: '#FEF0F0',
		iconColor: '#F56C6C',
	},
	{
		label: '检测通知',
		value: stats.value.typeCount?.DETECT || 0,
		icon: 'ele-Search',
		bgColor: '#ECF5FF',
		iconColor: '#409EFF',
	},
	{
		label: '批量检测',
		value: stats.value.typeCount?.BATCH_DETECT || 0,
		icon: 'ele-Document',
		bgColor: '#F0F9EB',
		iconColor: '#67C23A',
	},
]);

const getStats = () => {
	request.get('/api/message/stats').then(res => {
		if (res.code == 0) {
			stats.value = res.data;
		} else {
			ElMessage.error(res.msg || '获取统计数据失败');
		}
	}).catch(() => {
		ElMessage.error('获取统计数据失败');
	});
};

const getUserCount = () => {
	request.get('/api/message/users').then(res => {
		if (res.code == 0) {
			userCount.value = res.data.length;
		}
	});
};

const refreshStats = () => {
	getStats();
	getUserCount();
	ElMessage.success('数据已刷新');
};

onMounted(() => {
	getStats();
	getUserCount();
});
</script>

<style scoped lang="scss">
.message-stats-container {
	height: 100%;

	.stats-content {
		padding: 20px;
		overflow-y: auto;
	}
}

.stats-row {
	margin-bottom: 20px;
}

.stats-card {
	height: 140px;
	transition: all 0.3s;

	&:hover {
		transform: translateY(-5px);
	}

	.card-content {
		display: flex;
		align-items: center;
		height: 100%;
	}

	.card-icon {
		margin-right: 20px;
	}

	.card-info {
		flex: 1;
	}

	.card-value {
		font-size: 36px;
		font-weight: 700;
		line-height: 1.2;
	}

	.card-label {
		font-size: 14px;
		color: #909399;
		margin-top: 8px;
	}
}

.total-card {
	.card-value {
		color: #409EFF;
	}
}

.unread-card {
	.card-value {
		color: #E6A23C;
	}
}

.announce-card {
	.card-value {
		color: #67C23A;
	}
}

.rate-card {
	.card-value {
		color: #F56C6C;
	}
}

.chart-card {
	height: 100%;
	min-height: 350px;

	.chart-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.chart-title {
		font-size: 16px;
		font-weight: 600;
	}
}

.chart-container {
	padding: 10px 0;
}

.type-list {
	display: flex;
	flex-direction: column;
	gap: 20px;
	padding: 20px;
}

.type-item {
	display: flex;
	align-items: center;
	gap: 15px;
}

.type-info {
	width: 120px;
	display: flex;
	align-items: center;
	gap: 10px;
}

.type-count {
	font-size: 13px;
	color: #606266;
}

.type-bar {
	flex: 1;
	height: 20px;
	background: #f5f7fa;
	border-radius: 10px;
	overflow: hidden;
}

.type-bar-inner {
	height: 100%;
	border-radius: 10px;
	transition: width 0.6s ease;
}

.type-percentage {
	width: 50px;
	text-align: right;
	font-size: 14px;
	font-weight: 600;
	color: #303133;
}

.announce-stats {
	padding: 30px;
	display: flex;
	flex-direction: column;
	gap: 20px;
}

.announce-item {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 15px 20px;
	background: #f8f9fa;
	border-radius: 8px;
}

.announce-label {
	font-size: 14px;
	color: #606266;
}

.announce-value {
	font-size: 24px;
	font-weight: 700;
	color: #303133;

	&.read {
		color: #67C23A;
	}

	&.unread {
		color: #F56C6C;
	}
}

.announce-progress {
	padding: 20px 0;

	.progress-label {
		display: block;
		text-align: center;
		margin-top: 10px;
		font-size: 14px;
		color: #909399;
	}
}

.overview-container {
	display: flex;
	justify-content: space-around;
	flex-wrap: wrap;
	padding: 30px;
	gap: 20px;
}

.overview-item {
	display: flex;
	align-items: center;
	gap: 15px;
	padding: 20px;
	background: #fff;
	border-radius: 8px;
	box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
	min-width: 180px;
}

.overview-icon {
	width: 60px;
	height: 60px;
	border-radius: 12px;
	display: flex;
	align-items: center;
	justify-content: center;
}

.overview-info {
	flex: 1;
}

.overview-value {
	font-size: 28px;
	font-weight: 700;
	color: #303133;
	line-height: 1.2;
}

.overview-label {
	font-size: 14px;
	color: #909399;
	margin-top: 5px;
}
</style>
