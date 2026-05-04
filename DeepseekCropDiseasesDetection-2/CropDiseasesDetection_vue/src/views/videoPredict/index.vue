<template>
	<div class="system-predict-container layout-padding">
		<div class="system-predict-padding layout-padding-auto layout-padding-view">
			<div class="header">
				<div class="weight">
					<el-select v-model="weight" placeholder="请选择模型" size="large" style="width: 200px">
						<el-option v-for="item in state.weight_items" :key="item.value" :label="item.label"
							:value="item.value" />
					</el-select>
				</div>
				<div class="conf" style="margin-left: 20px;display: flex; flex-direction: row;">
					<div
						style="font-size: 14px;margin-right: 20px;display: flex;justify-content: start;align-items: center;color: #909399;">
						设置最小置信度阈值</div>
					<el-slider v-model="conf" :format-tooltip="formatTooltip" style="width: 300px;" />
				</div>
				<el-upload v-model="state.form.inputVideo" ref="uploadFile" class="avatar-uploader"
					action="http://localhost:9999/files/upload" :show-file-list="false"
					:on-success="handleAvatarSuccessone">
					<div class="button-section" style="margin-left: 20px">
						<el-button type="info" class="predict-button">上传视频</el-button>
					</div>
				</el-upload>
				<div class="button-section" style="margin-left: 20px">
					<el-button type="primary" @click="upData" class="predict-button">开始处理</el-button>
				</div>
				<div class="demo-progress" v-if="state.isShow">
					<el-progress :text-inside="true" :stroke-width="20" :percentage=state.percentage style="width: 400px;">
						<span>{{ state.type_text }} {{ state.percentage }}%</span>
					</el-progress>
				</div>
			</div>
			<div class="cards" ref="cardsContainer">
				<img v-if="state.video_path"
					 class="video"
					 :src="state.video_path"
					 @error="handleVideoError">
			</div>
		</div>
	</div>
</template>


<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import request from '/@/utils/request';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';
import type { UploadInstance, UploadProps } from 'element-plus';
import { SocketService } from '/@/utils/socket';
import { formatDate } from '/@/utils/formatTime';

import { Session } from '/@/utils/storage';

const uploadFile = ref<UploadInstance>();
const stores = useUserInfo();
const conf = ref(50);
const weight = ref('');
const { userInfos } = storeToRefs(stores);

const handleAvatarSuccessone: UploadProps['onSuccess'] = (response, uploadFile) => {
	ElMessage.success('上传成功！');
	state.form.inputVideo = response.data;
};
const state = reactive({
	weight_items: [] as any,
	data: {} as any,
	video_path: '',
	type_text: "正在保存",
	percentage: 50,
	isShow: false,
	form: {
		username: '',
		inputVideo: null as any,
		weight: '',
		conf: null as any,
		startTime: ''
	},
});

const socketService = new SocketService();

socketService.on('message', (data) => {
	console.log('Received message:', data);
	ElMessage.success(data);
});

const formatTooltip = (val: number) => {
	return val / 100
}

socketService.on('progress', (data) => {
	state.percentage = parseInt(data);
	if (parseInt(data) < 100) {
		state.isShow = true;
	} else {
		//两秒后隐藏进度条
		ElMessage.success("保存成功！");
		setTimeout(() => {
			state.isShow = false;
			state.percentage = 0;
		}, 2000);
	}
	console.log('Received message:', data);
});

const getData = () => {
	request.get('/api/flask/file_names').then((res) => {
		if (res.code == 0) {
			res.data = JSON.parse(res.data);
			console.log(res.data);
			state.weight_items = res.data.weight_items;
		} else {
			ElMessage.error(res.msg);
		}
	});
};


// 处理视频流加载失败（例如 Flask 返回 400, 415, 500）
const handleVideoError = () => {
    state.video_path = ''; // 停止请求
    ElMessage.closeAll(); // 关闭之前那个“正在加载”的提示
    ElMessage.error('视频处理失败：请检查文件格式是否正确（仅支持视频文件）');
};

const upData = () => {
    // 1. 校验模型是否选择
    if (!weight.value) {
        ElMessage.warning('请选择识别模型！');
        return;
    }

    // 2. 校验是否上传了文件
    if (!state.form.inputVideo || state.form.inputVideo === 'null') {
        ElMessage.warning('请先上传视频文件！');
        return;
    }

    // 3. 前端初步校验：如果文件名看起来像图片，直接拦截
    const imgExtensions = ['.jpg', '.jpeg', '.png', '.bmp', '.webp'];
    const isImage = imgExtensions.some(ext => state.form.inputVideo.toLowerCase().endsWith(ext));
    if (isImage) {
        ElMessage.error('当前是视频识别模块，请勿上传图片文件！');
        return;
    }

    // 执行后续逻辑
    state.form.weight = weight.value;
    state.form.conf = parseFloat(conf.value.toString()) / 100;
    state.form.username = userInfos.value.userName;
    state.form.startTime = formatDate(new Date(), 'YYYY-mm-dd HH:MM:SS');
    
    const queryParams = new URLSearchParams({
        ...state.form,
        token: Session.get('token')
    }).toString();
    state.video_path = `http://127.0.0.1:5000/predictVideo?${queryParams}`;
    
    // 提示正在加载
    ElMessage({
        message: '正在加载预览流，请稍候...',
        type: 'success',
        duration: 2000 // 设置一个较短的自动消失时间，或者配合 handleVideoError 关闭
    });
};

onMounted(() => {
	getData();
});
</script>

<style scoped lang="scss">
.predict-button {
	background: #4CAF50;
    border-color: #4CAF50;
    &:hover {
        background: #2E7D32;
        border-color: #2E7D32;
    }
}
.system-predict-container {
	width: 100%;
	height: 100%;
	display: flex;
	flex-direction: column;

	.system-predict-padding {
		padding: 15px;

		.el-table {
			flex: 1;
		}
	}
}

.header {
	width: 100%;
	height: 5%;
	display: flex;
	justify-content: start;
	align-items: center;
	font-size: 20px;
}

.cards {
	width: 100%;
	height: 95%;
	border-radius: 5px;
	margin-top: 15px;
	padding: 0px;
	overflow: hidden;
	display: flex;
	justify-content: center;
	align-items: center;
	/* 防止视频溢出 */
}

.video {
	width: 100%;
	max-height: 100%;
	/* 限制视频最大高度不超过父元素高度 */
	height: auto;
	object-fit: contain;
}

.button-section {
	display: flex;
	justify-content: center;
}

.predict-button {
	width: 100%;
	/* 按钮宽度填满 */
}

.demo-progress .el-progress--line {
	margin-left: 20px;
	width: 600px;
}
</style>