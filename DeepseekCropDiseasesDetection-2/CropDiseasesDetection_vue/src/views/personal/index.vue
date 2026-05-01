<template>
	<div class="system-role-container layout-padding">
		<div class="system-role-dialog-container">
			<el-card shadow="hover" header="个人信息" class="cards">
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
						<el-col :span="24" class="mb20">
							<el-form-item label="账号" prop="username">
								<el-input v-model="state.form.username" placeholder="请输入账号" clearable></el-input>
							</el-form-item>
						</el-col>
						<el-col :span="24" class="mb20">
							<el-form-item label="密码" prop="password">
								<el-input v-model="state.form.password" placeholder="留空则不修改密码" show-password clearable></el-input>
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
	</div>
</template>

<script setup lang="ts" name="personal">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage, type FormInstance } from 'element-plus';
import request from '/@/utils/request';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';
import { Plus } from '@element-plus/icons-vue';

const roleDialogFormRef = ref<FormInstance>();
const imageUrl = ref('');
const state = reactive({
	form: {} as any,
});

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);

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

		request.post('/api/user/update', postData).then((res) => {
			if (res.code == 0) {
				ElMessage.success('修改成功！');
				getTableData();
			} else {
				ElMessage.error(res.msg);
			}
		});
	});
};

onMounted(() => {
	getTableData();
});
</script>

<style scoped lang="scss">
.system-role-container {
	display: flex;
	align-items: center;
}
.system-role-dialog-container{
	width: 60%;
}

.cards{
	border-radius: 10px;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.el-form {
	width: 75%;
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
