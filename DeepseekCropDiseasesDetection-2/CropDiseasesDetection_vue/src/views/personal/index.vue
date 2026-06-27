<template>
	<div class="personal-container layout-padding">
		<el-tabs v-model="activeTab" class="personal-tabs">
			<el-tab-pane label="个人信息" name="info">
				<template #label>
					<span class="tab-label">
						<el-icon><ele-User /></el-icon>
						<span>个人信息</span>
					</span>
				</template>
			</el-tab-pane>
			<el-tab-pane label="消息设置" name="message">
				<template #label>
					<span class="tab-label">
						<el-icon><ele-Setting /></el-icon>
						<span>消息设置</span>
					</span>
				</template>
			</el-tab-pane>
		</el-tabs>

		<!-- 个人信息 -->
		<div v-if="activeTab === 'info'" class="tab-content">
			<el-card shadow="hover" header="个人信息" class="info-card">
				<el-form ref="roleDialogFormRef" :model="state.form" :rules="rules" size="default" label-width="100px">
					<el-row :gutter="35">
						<el-col :span="24" class="mb20">
							<el-form-item label="头像：">
								<el-upload
									class="avatar-uploader"
									action="http://localhost:9999/files/upload"
									:show-file-list="false"
									:on-success="handleAvatarSuccessone"
								>
									<img v-if="imageUrl" :src="imageUrl" class="avatar" />
									<el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
								</el-upload>
							</el-form-item>
						</el-col>
						<el-col :span="12" class="mb20">
							<el-form-item label="账号" prop="username">
								<el-input v-model="state.form.username" placeholder="请输入账号" clearable></el-input>
							</el-form-item>
						</el-col>
						<el-col :span="12" class="mb20">
							<el-form-item label="密码" prop="password">
								<el-input v-model="state.form.password" placeholder="留空则不修改密码" show-password clearable></el-input>
							</el-form-item>
						</el-col>
						<el-col :span="12" class="mb20">
							<el-form-item label="姓名" prop="name">
								<el-input v-model="state.form.name" placeholder="请输入姓名" clearable></el-input>
							</el-form-item>
						</el-col>
						<el-col :span="12" class="mb20">
							<el-form-item label="性别" prop="sex">
								<el-radio-group v-model="state.form.sex">
									<el-radio label="男">男</el-radio>
									<el-radio label="女">女</el-radio>
								</el-radio-group>
							</el-form-item>
						</el-col>
						<el-col :span="12" class="mb20">
							<el-form-item label="Email" prop="email">
								<el-input v-model="state.form.email" placeholder="请输入Email" clearable></el-input>
							</el-form-item>
						</el-col>
						<el-col :span="12" class="mb20">
							<el-form-item label="手机号码" prop="tel">
								<el-input v-model="state.form.tel" placeholder="请输入手机号码" clearable></el-input>
							</el-form-item>
						</el-col>
						<el-col :span="24" class="mb20">
							<el-form-item label="角色">
								<el-input v-model="state.form.role" disabled></el-input>
							</el-form-item>
						</el-col>
					</el-row>
				</el-form>
				<div style="text-align: right; margin-right: 15%">
					<el-button type="primary" @click="upData">确认修改</el-button>
				</div>
			</el-card>
		</div>

		<!-- 消息设置 -->
		<div v-if="activeTab === 'message'" class="tab-content">
			<el-card shadow="hover" class="message-card">
				<template #header>
					<div class="card-header">
						<span>消息通知偏好设置</span>
						<el-tag type="info">设置您的消息接收偏好</el-tag>
					</div>
				</template>

				<div class="message-settings">
					<!-- 免打扰模式 -->
					<div class="setting-section">
						<h4 class="section-title">
							<el-icon><ele-Mute /></el-icon>
							免打扰模式
						</h4>
						<div class="setting-item dnd-item">
							<div class="setting-info">
								<div class="setting-label">开启免打扰</div>
								<div class="setting-desc">开启后将暂停接收所有通知（安全警告除外）</div>
							</div>
							<el-switch
								v-model="messageSettings.doNotDisturb"
								@change="onDndChange"
								active-text="已开启"
								inactive-text="已关闭"
							/>
						</div>
						<div v-if="messageSettings.doNotDisturb" class="dnd-duration">
							<span class="duration-label">免打扰时长：</span>
							<el-radio-group v-model="dndDuration" @change="onDndDurationChange">
								<el-radio-button label="1">1小时</el-radio-button>
								<el-radio-button label="2">2小时</el-radio-button>
								<el-radio-button label="4">4小时</el-radio-button>
								<el-radio-button label="0">手动关闭</el-radio-button>
							</el-radio-group>
						</div>
					</div>

					<el-divider />

					<!-- 通知类型开关 -->
					<div class="setting-section">
						<h4 class="section-title">
							<el-icon><ele-Bell /></el-icon>
							通知类型设置
						</h4>
						<div class="setting-item">
							<div class="setting-info">
								<div class="setting-label">
									<el-icon color="#E6A23C"><ele-Notification /></el-icon>
									系统公告
								</div>
								<div class="setting-desc">接收系统发布的公告通知</div>
							</div>
							<el-switch v-model="messageSettings.announceNotify" @change="saveSettings" />
						</div>
						<div class="setting-item">
							<div class="setting-info">
								<div class="setting-label">
									<el-icon color="#F56C6C"><ele-WarnTriangleFilled /></el-icon>
									安全警告
								</div>
								<div class="setting-desc">接收系统安全相关的警告通知（建议保持开启）</div>
							</div>
							<el-switch v-model="messageSettings.securityNotify" @change="saveSettings" />
						</div>
						<div class="setting-item">
							<div class="setting-info">
								<div class="setting-label">
									<el-icon color="#409EFF"><ele-Search /></el-icon>
									检测通知
								</div>
								<div class="setting-desc">接收单张图片检测完成的通知</div>
							</div>
							<el-switch v-model="messageSettings.detectNotify" @change="saveSettings" />
						</div>
						<div class="setting-item">
							<div class="setting-info">
								<div class="setting-label">
									<el-icon color="#67C23A"><ele-Document /></el-icon>
									批量检测
								</div>
								<div class="setting-desc">接收批量检测完成的通知</div>
							</div>
							<el-switch v-model="messageSettings.batchNotify" @change="saveSettings" />
						</div>
					</div>

					<el-divider />

					<!-- 快捷操作 -->
					<div class="setting-section">
						<h4 class="section-title">
							<el-icon><ele-Operation /></el-icon>
							快捷操作
						</h4>
						<div class="quick-actions">
							<el-button type="primary" @click="enableAll">
								<el-icon><ele-CircleCheck /></el-icon>
								开启所有通知
							</el-button>
							<el-button type="warning" @click="disableAll">
								<el-icon><ele-CircleClose /></el-icon>
								关闭所有通知
							</el-button>
							<el-button type="success" @click="goToMessageCenter">
								<el-icon><ele-Bell /></el-icon>
								进入消息中心
							</el-button>
						</div>
					</div>
				</div>
			</el-card>
		</div>
	</div>
</template>

<script setup lang="ts" name="personal">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus';
import request from '/@/utils/request';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';
import { Plus } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const activeTab = ref('info');
const roleDialogFormRef = ref<FormInstance>();
const imageUrl = ref('');
const state = reactive({
	form: {} as any,
});

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);

// 消息设置相关
const messageSettings = reactive({
	detectNotify: true,
	batchNotify: true,
	announceNotify: true,
	securityNotify: true,
	doNotDisturb: false,
});

const dndDuration = ref('0');

// 校验规则
const rules = reactive({
	username: [{ required: true, message: '账号不能为空', trigger: 'blur' }],
	email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: ['blur', 'change'] }],
	tel: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
});

const handleAvatarSuccessone = (response: any) => {
	imageUrl.value = response.data;
	state.form.avatar = response.data;
};

const getTableData = () => {
	request.get('/api/user/' + userInfos.value.userName).then((res) => {
		if (res.code == 0) {
			state.form = res.data;
			// 角色显示转换
			const roleMap: any = { admin: '管理员', common: '普通用户', others: '其他用户' };
			state.form.role = roleMap[state.form.role] || state.form.role;
			imageUrl.value = state.form.avatar;
		}
	});
};

const upData = () => {
	roleDialogFormRef.value?.validate((valid) => {
		if (!valid) return ElMessage.error('请检查表单填写是否正确');

		// 角色转换回后端识别的标识
		const roleBackMap: any = { 管理员: 'admin', 普通用户: 'common', 其他用户: 'others' };
		const postData = { ...state.form };
		postData.role = roleBackMap[postData.role] || postData.role;

		request.put('/api/user', postData).then((res) => {
			if (res.code == 0) {
				ElMessage.success('修改成功！');
				getTableData();
			} else {
				ElMessage.error(res.msg);
			}
		});
	});
};

// 获取消息设置
const getMessageSettings = () => {
	request.get('/api/message/settings').then(res => {
		if (res.code == 0 && res.data) {
			Object.assign(messageSettings, res.data);
		}
	});
};

// 保存消息设置
const saveSettings = () => {
	request.put('/api/message/settings', messageSettings).then(res => {
		if (res.code == 0) {
			ElMessage.success('设置已保存');
		} else {
			ElMessage.error('保存失败');
		}
	});
};

// 免打扰开关变化
const onDndChange = (val: boolean) => {
	if (val) {
		// 开启免打扰，默认2小时
		dndDuration.value = '2';
		messageSettings.doNotDisturb = true;
	} else {
		messageSettings.doNotDisturb = false;
		dndDuration.value = '0';
	}
	saveSettings();
};

// 免打扰时长变化
const onDndDurationChange = (val: string) => {
	if (val === '0') {
		// 手动关闭
		messageSettings.doNotDisturb = false;
	} else {
		messageSettings.doNotDisturb = true;
	}
	saveSettings();
};

// 开启所有通知
const enableAll = () => {
	messageSettings.detectNotify = true;
	messageSettings.batchNotify = true;
	messageSettings.announceNotify = true;
	messageSettings.securityNotify = true;
	saveSettings();
};

// 关闭所有通知
const disableAll = () => {
	ElMessageBox.confirm('关闭所有通知后将无法接收任何消息提醒，确定要关闭吗？', '提示', {
		type: 'warning',
	}).then(() => {
		messageSettings.detectNotify = false;
		messageSettings.batchNotify = false;
		messageSettings.announceNotify = false;
		messageSettings.securityNotify = false;
		saveSettings();
	}).catch(() => {});
};

// 跳转到消息中心
const goToMessageCenter = () => {
	router.push('/message');
};

onMounted(() => {
	getTableData();
	getMessageSettings();
});
</script>

<style scoped lang="scss">
.personal-container {
	height: 100%;
	display: flex;
	flex-direction: column;
}

.personal-tabs {
	margin-bottom: 20px;

	.tab-label {
		display: flex;
		align-items: center;
		gap: 6px;
	}
}

.tab-content {
	height: calc(100% - 60px);
	overflow-y: auto;
}

.info-card {
	max-width: 900px;
	margin: 0 auto;

	.el-form {
		padding: 20px;
	}
}

.message-card {
	max-width: 800px;
	margin: 0 auto;

	.card-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}
}

.message-settings {
	padding: 10px 0;
}

.setting-section {
	margin-bottom: 10px;
}

.section-title {
	display: flex;
	align-items: center;
	gap: 8px;
	margin: 0 0 20px 0;
	font-size: 16px;
	color: #303133;
}

.setting-item {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 15px 20px;
	background: #f8f9fa;
	border-radius: 8px;
	margin-bottom: 12px;
	transition: all 0.3s;

	&:hover {
		background: #ecf5ff;
	}
}

.dnd-item {
	background: #fdf6ec;

	&:hover {
		background: #faecd8;
	}
}

.setting-info {
	flex: 1;
}

.setting-label {
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 15px;
	font-weight: 500;
	color: #303133;
	margin-bottom: 5px;
}

.setting-desc {
	font-size: 13px;
	color: #909399;
}

.dnd-duration {
	margin-top: 15px;
	padding: 15px 20px;
	background: #fdf6ec;
	border-radius: 8px;

	.duration-label {
		font-size: 14px;
		color: #606266;
		margin-right: 15px;
	}
}

.quick-actions {
	display: flex;
	gap: 15px;
	flex-wrap: wrap;
}

:deep(.el-radio-button__inner) {
	padding: 8px 15px;
}

.avatar-uploader .el-upload:hover {
	border-color: #409eff;
}

.avatar {
	width: 120px;
	height: 120px;
	display: block;
	border-radius: 8px;
}

.avatar-uploader-icon {
	font-size: 28px;
	color: #8c939d;
	width: 120px;
	height: 120px;
	display: flex;
	justify-content: center;
	align-items: center;
	border: 1px dashed #d9d9d9;
	border-radius: 8px;
	cursor: pointer;
}
</style>
