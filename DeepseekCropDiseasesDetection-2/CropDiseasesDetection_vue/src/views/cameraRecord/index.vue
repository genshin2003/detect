<template>
	<div class="system-role-container layout-padding">
		<div class="system-role-padding layout-padding-auto layout-padding-view">
			<div class="system-user-search mb15">
				<el-input v-model="state.tableData.param.startTime" size="default" placeholder="请输入识别时间" style="max-width: 180px"> </el-input>
				<el-input v-model="state.tableData.param.conf" size="default" placeholder="请输入最低阈值" style="max-width: 180px; margin-left: 15px"></el-input>
				<el-button size="default" type="primary" class="predict-button" @click="getTableData()">
					<el-icon>
						<ele-Search />
					</el-icon>
					查询
				</el-button>

				<el-button size="default" type="success" class="predict-button" style="margin-left: 10px" @click="onExportExcel">
					导出Excel
				</el-button>
			</div>
			<el-table :data="state.tableData.data" v-loading="state.tableData.loading" style="width: 100%">
				<el-table-column prop="num" label="序号" width="100" align="center" />
				<el-table-column prop="outVideo" label="处理结果" width="200" align="center">
					<template #default="scope">
						<video class="video" preload="auto" controls :key="scope.row.outVideo + uniqueKey">
							<source :src="scope.row.outVideo" type="video/mp4" />
						</video>
					</template>
				</el-table-column>
				<el-table-column prop="weight" label="识别权重" align="center" />
				<el-table-column prop="conf" label="最小阈值" show-overflow-tooltip width="100" align="center"></el-table-column>
				<el-table-column prop="username" label="识别用户" show-overflow-tooltip align="center"></el-table-column>
				<el-table-column prop="startTime" label="识别时间" show-overflow-tooltip align="center"></el-table-column>
				<el-table-column label="操作" width="350" align="center">
					<template #default="scope">
						<el-button size="small" text type="warning" @click="onToggleFavorite(scope.row)">
							{{ scope.row._favorited ? '★' : '☆' }}
						</el-button>
						<el-button size="small" text type="primary" @click="onExportPdf(scope.row)">导出PDF</el-button>
						<el-button size="small" text type="primary" @click="onRowDel(scope.row)">删除</el-button>
					</template>
				</el-table-column>
			</el-table>
			<el-pagination @size-change="onHandleSizeChange" @current-change="onHandleCurrentChange" class="mt15"
				:pager-count="5" :page-sizes="[10, 20, 30]" v-model:current-page="state.tableData.param.pageNum"
				background v-model:page-size="state.tableData.param.pageSize"
				layout="total, sizes, prev, pager, next, jumper" :total="state.tableData.total">
			</el-pagination>

			<!-- 收藏弹窗 -->
			<el-dialog v-model="favDialogVisible" title="收藏" width="400px">
				<el-form :model="favForm" label-width="70px">
					<el-form-item label="收藏夹">
						<el-select v-model="favForm.folderId" style="width: 100%">
							<el-option v-for="f in favFolders" :key="f.id" :label="f.name" :value="f.id" />
						</el-select>
					</el-form-item>
					<el-form-item label="备注">
						<el-input v-model="favForm.remark" type="textarea" :rows="2" />
					</el-form-item>
					<el-form-item label="优先级">
						<el-select v-model="favForm.priority" style="width: 100%">
							<el-option label="普通" value="NORMAL" />
							<el-option label="重要" value="IMPORTANT" />
							<el-option label="特别重要" value="CRITICAL" />
						</el-select>
					</el-form-item>
				</el-form>
				<template #footer>
					<el-button @click="favDialogVisible = false">取消</el-button>
					<el-button type="primary" @click="onSaveFavorite">保存</el-button>
				</template>
			</el-dialog>
		</div>
	</div>
</template>

<script setup lang="ts" name="systemRole">
import { reactive, onMounted, ref } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import request from '/@/utils/request';
import { useUserInfo } from '/@/stores/userInfo';
import { storeToRefs } from 'pinia';
import { downloadPdf, downloadExcel } from '/@/utils/download';

const stores = useUserInfo();
const { userInfos } = storeToRefs(stores);

const state = reactive<SysRoleState>({
	tableData: {
		data: [] as any,
		total: 0,
		loading: false,
		param: {
			username: '',
			conf: '',
			startTime: '',
			endTime: '',
			pageNum: 1,
			pageSize: 10,
		},
	},
});

// 唯一标识符，动态刷新
const uniqueKey = ref(0);

const getTableData = () => {
	state.tableData.loading = true;
	if (userInfos.value.userName != 'admin') {
		state.tableData.param.username = userInfos.value.userName;
	}
	request
		.get('/api/cameraRecords', {
			params: state.tableData.param,
		})
		.then((res) => {
			if (res.code == 0) {
				state.tableData.data = [];
				setTimeout(() => {
					state.tableData.loading = false;
				}, 500);
				for (let i = 0; i < res.data.records.length; i++) {
					state.tableData.data[i] = res.data.records[i];
					state.tableData.data[i]['num'] = i + 1;
				}
				state.tableData.total = res.data.total;

				// 更新唯一标识符
				uniqueKey.value++;
				checkFavoriteStatus();
			} else {
				ElMessage({
					type: 'error',
					message: res.msg,
				});
			}
		});
};

const onExportPdf = (row: any) => {
	downloadPdf('camera', row.id);
};

const onExportExcel = () => {
	const params: Record<string, string> = {};
	if (state.tableData.param.username) params.username = state.tableData.param.username;
	if (state.tableData.param.startTime) params.startTime = state.tableData.param.startTime;
	if (state.tableData.param.endTime) params.endTime = state.tableData.param.endTime;
	downloadExcel('camera', params);
};

const onRowDel = (row: any) => {
	ElMessageBox.confirm(`此操作将永久删除该信息，是否继续?`, '提示', {
		confirmButtonText: '确认',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(() => {
			request.delete('/api/cameraRecords/' + row.id).then((res) => {
				if (res.code == 0) {
					ElMessage({
						type: 'success',
						message: '删除成功！',
					});
				} else {
					ElMessage({
						type: 'error',
						message: res.msg,
					});
				}
			});
			setTimeout(() => {
				getTableData();
			}, 500);
		})
		.catch(() => { });
};

const onHandleSizeChange = (val: number) => {
	state.tableData.param.pageSize = val;
	getTableData();
};

const onHandleCurrentChange = (val: number) => {
	state.tableData.param.pageNum = val;
	getTableData();
};

// ========== 收藏功能 ==========
const favDialogVisible = ref(false);
const favFolders = ref<any[]>([]);
const favForm = ref({ recordId: 0, folderId: null as number | null, remark: '', tags: '', priority: 'NORMAL' });

const getFolders = () => {
	request.get('/api/favorite/folders').then(res => {
		if (res.code == 0) favFolders.value = res.data;
	});
};

const checkFavoriteStatus = () => {
	const ids = state.tableData.data.map((item: any) => item.id);
	if (ids.length === 0) return;
	request.get('/api/favorite/batchCheck', { params: { recordIds: ids.join(','), recordType: 'CAMERA' } }).then(res => {
		if (res.code == 0) {
			state.tableData.data.forEach((item: any) => {
				const fav = res.data[item.id];
				item._favorited = !!fav;
				item._favoriteId = fav ? fav.id : null;
			});
		}
	});
};

const onToggleFavorite = (row: any) => {
	if (row._favorited) {
		request.delete('/api/favorite/' + row._favoriteId).then(res => {
			if (res.code == 0) {
				row._favorited = false;
				row._favoriteId = null;
				ElMessage.success('已取消收藏');
			}
		});
	} else {
		favForm.value = { recordId: row.id, folderId: null, remark: '', tags: '', priority: 'NORMAL' };
		getFolders();
		favDialogVisible.value = true;
	}
};

const onSaveFavorite = () => {
	request.post('/api/favorite', { ...favForm.value, recordType: 'CAMERA' }).then(res => {
		if (res.code == 0) {
			ElMessage.success('收藏成功');
			favDialogVisible.value = false;
			getTableData();
		}
	});
};

onMounted(() => {
	getTableData();
});
</script>


<style scoped lang="scss">
.system-role-container {

	// background: radial-gradient(circle, #d3e3f1 0%, #ffffff 100%);
	.system-role-padding {
		padding: 15px;

		.el-table {
			flex: 1;
		}
	}
}

.video {
	width: 100%;
	max-height: 100%;
	/* 限制视频最大高度不超过父元素高度 */
	height: auto;
	object-fit: contain;
}
.predict-button {
	margin-left: 10px;
	background: #4CAF50;
    border-color: #4CAF50;
    &:hover {
        background: #2E7D32;
        border-color: #2E7D32;
    }
}
</style>
