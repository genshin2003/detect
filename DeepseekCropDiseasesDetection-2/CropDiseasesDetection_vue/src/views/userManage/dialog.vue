<template>
	<div class="system-role-dialog-container">
		<el-dialog :title="state.dialog.title" v-model="state.dialog.isShowDialog" width="600px">
			<el-form ref="roleDialogFormRef" :model="state.form" :rules="rules" size="default" label-width="100px">
				<el-row :gutter="20">
					<el-col :span="24" class="mb20" style="text-align: center">
						<el-upload
							class="avatar-uploader"
							action="http://localhost:9999/files/upload"
							:show-file-list="false"
							:on-success="handleAvatarSuccessone"
						>
							<img v-if="imageUrl" :src="imageUrl" class="avatar" />
							<el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
						</el-upload>
					</el-col>
					<el-col :span="24" class="mb20">
						<el-form-item label="账号" prop="username">
							<el-input v-model="state.form.username" placeholder="请输入账号" clearable></el-input>
						</el-form-item>
					</el-col>
					<el-col :span="24" class="mb20">
						<el-form-item label="密码" prop="password">
							<el-input v-model="state.form.password" placeholder="请输入密码" show-password clearable></el-input>
						</el-form-item>
					</el-col>
					<el-col :span="24" class="mb20">
						<el-form-item label="姓名" prop="name">
							<el-input v-model="state.form.name" placeholder="请输入姓名" clearable></el-input>
						</el-form-item>
					</el-col>
					<el-col :span="24" class="mb20">
						<el-form-item label="性别" prop="sex">
							<el-radio-group v-model="state.form.sex">
								<el-radio label="男">男</el-radio>
								<el-radio label="女">女</el-radio>
							</el-radio-group>
						</el-form-item>
					</el-col>
					<el-col :span="24" class="mb20">
						<el-form-item label="Email" prop="email">
							<el-input v-model="state.form.email" placeholder="请输入Email" clearable></el-input>
						</el-form-item>
					</el-col>
					<el-col :span="24" class="mb20">
						<el-form-item label="手机号码" prop="tel">
							<el-input v-model="state.form.tel" placeholder="请输入手机号码" clearable></el-input>
						</el-form-item>
					</el-col>
					<el-col :span="24" class="mb20">
						<el-form-item label="角色" prop="role">
							<el-select v-model="state.form.role" placeholder="请选择角色" style="width: 100%">
								<el-option v-for="item in option" :key="item.id" :label="item.label" :value="item.role" />
							</el-select>
						</el-form-item>
					</el-col>
				</el-row>
			</el-form>
			<template #footer>
				<span class="dialog-footer">
					<el-button @click="onCancel" size="default">取 消</el-button>
					<el-button type="primary" @click="onSubmit" size="default">{{ state.dialog.submitTxt }}</el-button>
				</span>
			</template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts" name="systemRoleDialog">
import { nextTick, reactive, ref } from 'vue';
import { ElMessage, type FormInstance } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import request from '/@/utils/request';

const emit = defineEmits(['refresh']);
const roleDialogFormRef = ref<FormInstance>();
const imageUrl = ref('');

const rules = reactive({
	username: [{ required: true, message: '账号不能为空', trigger: 'blur' }],
	password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
	name: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
	email: [{ type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] }],
	tel: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
	role: [{ required: true, message: '请选择角色', trigger: 'change' }],
});

const option = [
	{ id: 1, label: '管理员', role: 'admin' },
	{ id: 2, label: '普通用户', role: 'common' },
];

const state = reactive({
	form: {} as any,
	dialog: { isShowDialog: false, title: '', submitTxt: '' },
});

const handleAvatarSuccessone = (response: any) => {
	imageUrl.value = response.data;
	state.form.avatar = response.data;
};

const openDialog = (type: string, row?: any) => {
	if (type === 'edit') {
		state.form = { ...row };
		state.dialog.title = '修改信息';
		state.dialog.submitTxt = '修 改';
		imageUrl.value = state.form.avatar;
	} else {
		state.form = { sex: '男' }; // 默认性别
		state.dialog.title = '新增信息';
		state.dialog.submitTxt = '新 增';
		imageUrl.value = '';
	}
	state.dialog.isShowDialog = true;
	// 清除上一次的校验红字
	nextTick(() => {
		roleDialogFormRef.value?.clearValidate();
	});
};

const onSubmit = () => {
	roleDialogFormRef.value?.validate((valid) => {
		if (!valid) return;

		// 角色名称转换逻辑
		const roleBackMap: any = { 管理员: 'admin', 普通用户: 'common', 其他用户: 'others' };
		if (roleBackMap[state.form.role]) {
			state.form.role = roleBackMap[state.form.role];
		}

		const isEdit = state.dialog.title === '修改信息';
		const api = isEdit ? '/api/user/update' : '/api/user/';

		request.post(api, state.form).then((res) => {
			if (res.code == 0) {
				ElMessage.success(`${state.dialog.submitTxt}成功！`);
				state.dialog.isShowDialog = false;
				emit('refresh');
			} else {
				ElMessage.error(res.msg);
			}
		});
	});
};

const onCancel = () => {
	state.dialog.isShowDialog = false;
};

defineExpose({ openDialog });
</script>

<style scoped>
.avatar-uploader .avatar {
	width: 100px;
	height: 100px;
	display: block;
	margin: 0 auto;
}
</style>

<style scoped lang="scss">
:deep(.dia) {
	width: 800px;
	height: 650px;
	display: flex;
	flex-direction: column;
	justify-content: center;
	align-items: center;
}

.el-form {
	width: 80%;
	margin-left: 10%;
}

.imgs {
	font-size: 28px;
	color: hsl(215, 8%, 58%);
	width: 120px;
	height: 120px;
	display: flex;
	justify-content: center;
	align-items: center;
	border: 1px dashed #d9d9d9;
	border-radius: 6px;
	cursor: pointer;
	margin-left: 320px;
	margin-bottom: 20px;
}

.avatar-uploader .el-upload:hover {
	border-color: #409eff;
}
.avatar {
	width: 120px;
	height: 120px;
	display: block;
}
</style>
