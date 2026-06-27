<template>
	<div class="system-role-container layout-padding">
		<div class="system-role-padding layout-padding-auto layout-padding-view">
			<div class="publish-wrapper">
				<el-card class="publish-card">
					<template #header>
						<div class="card-header">
							<span class="card-title">发布公告</span>
							<el-tag type="info">管理员功能</el-tag>
						</div>
					</template>
					<el-form :model="form" label-width="100px" :rules="rules" ref="formRef" class="publish-form">
						<el-form-item label="消息类型" prop="type">
							<el-select v-model="form.type" placeholder="请选择消息类型" style="width: 100%">
								<el-option label="系统公告" value="ANNOUNCE">
									<div class="type-option">
										<el-icon><ele-Bell /></el-icon>
										<span>系统公告</span>
									</div>
								</el-option>
								<el-option label="安全警告" value="SECURITY">
									<div class="type-option">
										<el-icon><ele-WarnTriangleFilled /></el-icon>
										<span>安全警告</span>
									</div>
								</el-option>
								<el-option label="检测通知" value="DETECT">
									<div class="type-option">
										<el-icon><ele-Search /></el-icon>
										<span>检测通知</span>
									</div>
								</el-option>
								<el-option label="批量检测" value="BATCH_DETECT">
									<div class="type-option">
										<el-icon><ele-Document /></el-icon>
										<span>批量检测</span>
									</div>
								</el-option>
							</el-select>
						</el-form-item>
						<el-form-item label="公告标题" prop="title">
							<el-input v-model="form.title" placeholder="请输入公告标题" maxlength="50" show-word-limit />
						</el-form-item>
						<el-form-item label="公告内容" prop="content">
							<el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入公告内容" maxlength="500" show-word-limit />
						</el-form-item>
						<el-form-item label="优先级" prop="priority">
							<el-radio-group v-model="form.priority">
								<el-radio-button label="NORMAL">
									<el-icon><ele-InfoFilled /></el-icon>
									<span>普通</span>
								</el-radio-button>
								<el-radio-button label="HIGH" class="urgent-radio">
									<el-icon><ele-WarnTriangleFilled /></el-icon>
									<span>紧急</span>
								</el-radio-button>
							</el-radio-group>
							<transition name="el-fade-in">
								<span v-if="form.priority === 'HIGH'" class="urgent-warning">
									<el-icon><ele-WarnTriangleFilled /></el-icon>
									紧急公告将以醒目的红色样式展示给所有用户
								</span>
							</transition>
						</el-form-item>
						<el-form-item label="发送范围" prop="scope">
							<el-radio-group v-model="form.scope" @change="onScopeChange">
								<el-radio-button label="all">全体用户</el-radio-button>
								<el-radio-button label="specific">指定用户</el-radio-button>
							</el-radio-group>
						</el-form-item>
						<el-form-item v-if="form.scope === 'specific'" label="选择用户" prop="usernames">
							<el-select
								v-model="form.usernames"
								multiple
								filterable
								placeholder="请选择要发送的用户"
								style="width: 100%"
							>
								<el-option
									v-for="user in userList"
									:key="user.username"
									:label="user.name + ' (' + user.username + ')'"
									:value="user.username"
								/>
							</el-select>
						</el-form-item>
						<el-form-item label="预览">
							<div :class="['preview-box', { 'preview-urgent': form.priority === 'HIGH' }]">
								<div class="preview-header">
									<el-tag :type="getTypeTag(form.type)" size="small">{{ getTypeLabel(form.type) }}</el-tag>
									<el-tag v-if="form.priority === 'HIGH'" type="danger" size="small" effect="dark" style="margin-left: 8px">
										<el-icon><ele-WarnTriangleFilled /></el-icon> 紧急
									</el-tag>
									<el-tag type="info" size="small" style="margin-left: 8px">
										{{ form.scope === 'all' ? '全体用户' : '指定用户 (' + form.usernames.length + '人)' }}
									</el-tag>
								</div>
								<h4 :class="['preview-title', { 'urgent-title': form.priority === 'HIGH' }]">
									{{ form.priority === 'HIGH' ? '【紧急】' : '' }}{{ form.title || '公告标题' }}
								</h4>
								<p class="preview-content">{{ form.content || '公告内容将在这里显示...' }}</p>
							</div>
						</el-form-item>
						<el-form-item>
							<el-button type="primary" @click="onSubmit" :loading="submitting" size="large">
								<el-icon><ele-Promotion /></el-icon>
								发布公告
							</el-button>
							<el-button @click="onReset" size="large">重置</el-button>
						</el-form-item>
					</el-form>
				</el-card>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts" name="messagePublish">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, type FormInstance } from 'element-plus';
import request from '/@/utils/request';

const formRef = ref<FormInstance>();
const submitting = ref(false);
const userList = ref<any[]>([]);

const form = reactive({
	type: 'ANNOUNCE',
	title: '',
	content: '',
	priority: 'NORMAL',
	scope: 'all',
	usernames: [] as string[],
});

const rules = {
	type: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
	title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
	content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
};

const getTypeLabel = (type: string) => {
	const map: Record<string, string> = {
		DETECT: '检测通知',
		BATCH_DETECT: '批量检测',
		ANNOUNCE: '系统公告',
		SECURITY: '安全警告',
	};
	return map[type] || '系统公告';
};

const getTypeTag = (type: string) => {
	const map: Record<string, string> = {
		DETECT: 'primary',
		BATCH_DETECT: 'success',
		ANNOUNCE: 'warning',
		SECURITY: 'danger',
	};
	return map[type] || 'warning';
};

const onSubmit = () => {
	formRef.value?.validate((valid) => {
		if (!valid) return;
		submitting.value = true;
		request.post('/api/message/publish', form).then((res) => {
			submitting.value = false;
			if (res.code == 0) {
				ElMessage({ type: 'success', message: '公告发布成功' });
				onReset();
			} else {
				ElMessage({ type: 'error', message: res.msg || '发布失败' });
			}
		}).catch(() => {
			submitting.value = false;
			ElMessage({ type: 'error', message: '网络请求失败' });
		});
	});
};

const onReset = () => {
	form.type = 'ANNOUNCE';
	form.title = '';
	form.content = '';
	form.priority = 'NORMAL';
	form.scope = 'all';
	form.usernames = [];
};

const onScopeChange = (val: string) => {
	if (val === 'specific' && userList.value.length === 0) {
		getUserList();
	}
};

const getUserList = () => {
	request.get('/api/message/users').then(res => {
		if (res.code == 0) {
			userList.value = res.data;
		}
	});
};

onMounted(() => {
	// 预加载用户列表
	getUserList();
});
</script>

<style scoped lang="scss">
.system-role-container {
	.system-role-padding {
		padding: 20px;
	}
}

.publish-wrapper {
	display: flex;
	justify-content: center;
}

.publish-card {
	width: 100%;
	max-width: 900px;

	.card-header {
		display: flex;
		align-items: center;
		justify-content: space-between;

		.card-title {
			font-size: 20px;
			font-weight: 600;
		}
	}
}

.publish-form {
	padding: 20px 0;
}

.type-option {
	display: flex;
	align-items: center;
	gap: 8px;
}

.preview-box {
	width: 100%;
	padding: 20px;
	background: #f8f9fa;
	border-radius: 8px;
	border: 1px solid #e9ecef;
	transition: all 0.3s ease;

	.preview-header {
		margin-bottom: 12px;
	}

	.preview-title {
		margin: 0 0 10px 0;
		font-size: 16px;
		color: #333;
	}

	.preview-content {
		margin: 0;
		font-size: 14px;
		color: #666;
		line-height: 1.6;
		min-height: 60px;
	}
}

.preview-urgent {
	background: #fef0f0;
	border-color: #fbc4c4;
	box-shadow: 0 0 12px rgba(245, 108, 108, 0.2);

	.urgent-title {
		color: #f56c6c;
		font-weight: 600;
	}
}

.urgent-warning {
	display: inline-flex;
	align-items: center;
	gap: 4px;
	margin-left: 12px;
	color: #f56c6c;
	font-size: 12px;
	font-weight: 500;
}

.urgent-radio {
	:deep(.el-radio-button__inner) {
		color: #f56c6c;
		border-color: #f56c6c;
	}
}

:deep(.el-radio-button__inner) {
	display: flex;
	align-items: center;
	gap: 6px;
}
</style>
