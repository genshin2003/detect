<template>
	<div class="system-role-container layout-padding">
		<div class="system-role-padding layout-padding-auto layout-padding-view">
			<div class="system-user-search mb15">
				<!-- 时间查询（日期选择器） -->
				<el-date-picker v-model="searchTime"
								type="datetimerange"
								range-separator="至"
								start-placeholder="开始时间"
								end-placeholder="结束时间"
								style="max-width: 350px"
								value-format="YYYY-MM-DD HH:mm:ss"
								clearable />

				<!-- 识别种类查询 -->
				<el-input
					v-model="state.tableData.param.search2"
					size="default"
					placeholder="请输入识别结果"
					style="max-width: 180px; margin-left: 15px"
				/>

				<el-button size="default" type="primary" class="predict-button" @click="getTableData()">
					<el-icon>
						<ele-Search />
					</el-icon>
					查询
				</el-button>

				<el-button size="default" @click="resetSearch" style="margin-left: 10px">
					重置
				</el-button>
			</div>

			<el-table :data="state.tableData.data" style="width: 100%">
				<el-table-column type="expand">
					<template #default="props">
						<div m="4">
							<p style="margin-left: 20px; font-size: 16px; font-weight: 800;">详细识别结果：</p>
							<el-table :data="props.row.family">
								<el-table-column prop="label" label="识别结果" align="center" />
								<el-table-column prop="confidence" label="置信度" show-overflow-tooltip align="center" />
								<el-table-column prop="startTime" label="识别时间" align="center" />
							</el-table>
						</div>
					</template>
				</el-table-column>
				<el-table-column prop="num" label="序号" width="80" align="center" />
				<el-table-column prop="inputImg" label="原始图片" width="120" align="center">
					<template #default="scope">
						<el-image :src="scope.row.inputImg" :preview-src-list="state.tableData.data.map(item => item.inputImg)"
							:initial-index="scope.$index" fit="cover" style="width: 120px; height: 80px" preview-teleported />
					</template>
				</el-table-column>
				<el-table-column prop="outImg" label="预测图片" width="120" align="center">
					<template #default="scope">
						<el-image :src="scope.row.outImg" :preview-src-list="state.tableData.data.map(item => item.outImg)"
							:initial-index="scope.$index" fit="cover" style="width: 120px; height: 80px" preview-teleported />
					</template>
				</el-table-column>

				<el-table-column prop="label" label="识别结果" show-overflow-tooltip align="center" />

				<el-table-column prop="weight" label="识别权重" show-overflow-tooltip align="center" />
				<el-table-column prop="conf" label="最小阈值" show-overflow-tooltip align="center" />
				<el-table-column prop="ai" label="AI助手" show-overflow-tooltip align="center" />
				<el-table-column prop="suggestion" label="AI建议" show-overflow-tooltip align="center" />
				<el-table-column prop="startTime" label="识别时间" width="200" align="center" />
				<el-table-column prop="username" label="识别用户" show-overflow-tooltip align="center" />
				<el-table-column label="操作" width="80">
					<template #default="scope">
						<el-button size="small" text type="primary" @click="onRowDel(scope.row)">删除</el-button>
					</template>
				</el-table-column>
			</el-table>

			<el-pagination @size-change="onHandleSizeChange" @current-change="onHandleCurrentChange" class="mt15"
				:pager-count="5" :page-sizes="[10, 20, 30]" v-model:current-page="state.tableData.param.pageNum"
				background v-model:page-size="state.tableData.param.pageSize"
				layout="total, sizes, prev, pager, next, jumper" :total="state.tableData.total">
			</el-pagination>
		</div>
	</div>
</template>

<script setup lang="ts" name="systemRole">
import { reactive, onMounted, ref } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import request from '/@/utils/request';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);

    // 脚本部分：修改参数结构和赋值逻辑
    const searchTime = ref<any[]>([]); // 改为数组来接收范围值

    const state = reactive({
        tableData: {
            data: [] as any,
            total: 0,
            loading: false,
            param: {
                search: '',
                search1: '', // 依然作为开始时间
                endTime: '', // 新增：作为结束时间
                search2: '',
                pageNum: 1,
                pageSize: 10,
            },
        },
    });

    const getTableData = () => {
        state.tableData.loading = true;

        if (userInfos.value.userName !== 'admin') {
            state.tableData.param.search = userInfos.value.userName;
        }

        // 同步日期范围到请求参数
        if (searchTime.value && searchTime.value.length === 2) {
            state.tableData.param.search1 = searchTime.value[0]; // 开始时间
            state.tableData.param.endTime = searchTime.value[1]; // 结束时间
        } else {
            state.tableData.param.search1 = '';
            state.tableData.param.endTime = '';
        }

        request.get('/api/imgRecords', { params: state.tableData.param })
		.then((res) => {
			// === 关键修复点：使用 ==（松等）或 '0'，因为后端 code 是字符串 "0"
			if (res.code == 0) {          // ←←← 这里改成 == 0
				state.tableData.data = [];

				for (let i = 0; i < res.data.records.length; i++) {
					const confidences = JSON.parse(res.data.records[i].confidence);
					const labels = JSON.parse(res.data.records[i].label);

					const transformedData = transformData(res.data.records[i], confidences, labels);
					transformedData['num'] = i + 1;
					state.tableData.data[i] = transformedData;
				}

				state.tableData.total = res.data.total;
				setTimeout(() => {
					state.tableData.loading = false;
				}, 300);
			} else {
				ElMessage({
					type: 'error',
					message: res.msg || '请求失败',
				});
			}
		})
		.catch(() => {
			ElMessage({ type: 'error', message: '网络请求失败' });
		});
};

const resetSearch = () => {
    searchTime.value = []; // 修改这里：将 '' 改为 []
    state.tableData.param.search1 = '';
    state.tableData.param.endTime = ''; // 如果你按前面的建议加了 endTime 的话
    state.tableData.param.search2 = '';
    getTableData();
};

const transformData = (originalData: any, confidences: any[], labels: any[]) => {
	const family = labels.map((label: string, index: number) => ({
		label: label,
		confidence: confidences[index],
		startTime: originalData.startTime,
	}));

	return {
		id: originalData.id,
		inputImg: originalData.inputImg,
		outImg: originalData.outImg,
		weight: originalData.weight,
		allTime: originalData.allTime,
		conf: originalData.conf,
		startTime: originalData.startTime,
		username: originalData.username,
		ai: originalData.ai,
		suggestion: originalData.suggestion,
		family: family,
		label: labels.length > 0 ? labels[0] : '-',
	};
};

const onRowDel = (row: any) => {
	ElMessageBox.confirm('此操作将永久删除该信息，是否继续?', '提示', {
		confirmButtonText: '确认',
		cancelButtonText: '取消',
		type: 'warning',
	}).then(() => {
		request.delete('/api/imgRecords/' + row.id).then((res) => {
			if (res.code == 0) {
				ElMessage({ type: 'success', message: '删除成功！' });
			} else {
				ElMessage({ type: 'error', message: res.msg });
			}
		});
		setTimeout(() => getTableData(), 500);
	});
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
</style>
