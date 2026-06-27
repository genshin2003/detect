<template>
	<div class="favorite-container layout-padding">
		<div class="favorite-wrapper">
			<!-- 顶部Tab切换 -->
			<div class="favorite-tabs-wrapper">
				<el-tabs v-model="activeTab" class="favorite-tabs" @tab-change="onTabChange">
					<el-tab-pane name="my">
						<template #label>
							<span class="tab-label">
								<el-icon><ele-Star /></el-icon>
								<span>我的收藏</span>
							</span>
						</template>
					</el-tab-pane>
					<el-tab-pane name="public">
						<template #label>
							<span class="tab-label">
								<el-icon><ele-Globe /></el-icon>
								<span>公开收藏夹</span>
							</span>
						</template>
					</el-tab-pane>
				</el-tabs>
			</div>

			<!-- 我的收藏 -->
			<div v-if="activeTab === 'my'" class="favorite-layout">
				<!-- 左侧收藏夹列表 -->
				<div class="favorite-sidebar">
					<div class="sidebar-header">
						<span style="font-weight: 600">我的收藏夹</span>
						<el-button size="small" type="primary" @click="onCreateFolder">新建</el-button>
					</div>
					<div class="folder-list">
						<!-- 全部收藏 -->
						<div
							:class="['folder-item', { active: currentFolderId === null }]"
							@click="onSelectFolder(null)"
						>
							<div class="folder-info">
								<el-icon><ele-Collection /></el-icon>
								<span class="folder-name">全部收藏</span>
								<span class="folder-count">{{ totalCount }}</span>
							</div>
						</div>
						<!-- 各个收藏夹 -->
						<div
							v-for="folder in folders"
							:key="folder.id"
							:class="['folder-item', { active: currentFolderId === folder.id }]"
							@click="onSelectFolder(folder.id)"
						>
							<div class="folder-info">
								<el-icon v-if="folder.isPublic" class="public-icon"><ele-Globe /></el-icon>
								<el-icon v-else><ele-Folder /></el-icon>
								<span class="folder-name">{{ folder.name }}</span>
								<span class="folder-count">{{ folder.count }}</span>
							</div>
							<div class="folder-actions" v-if="folder.name !== '默认收藏夹'">
								<el-icon @click.stop="onEditFolder(folder)"><ele-Edit /></el-icon>
								<el-icon @click.stop="onDeleteFolder(folder)"><ele-Delete /></el-icon>
							</div>
							<div class="folder-actions" v-else>
								<el-icon @click.stop="onEditFolder(folder)"><ele-Edit /></el-icon>
							</div>
						</div>
					</div>
				</div>

				<!-- 右侧收藏记录列表 -->
				<div class="favorite-main">
					<div class="main-header">
						<div class="header-left">
							<span style="font-weight: 600; font-size: 16px; margin-right: 15px">
								{{ currentFolderName }}
							</span>
							<el-select v-model="filterPriority" placeholder="优先级筛选" clearable style="width: 130px; margin-right: 10px" @change="getFavorites()">
								<el-option label="普通" value="NORMAL" />
								<el-option label="重要" value="IMPORTANT" />
								<el-option label="特别重要" value="CRITICAL" />
							</el-select>
						</div>
						<div class="header-right">
							<el-button size="small" type="danger" :disabled="selectedIds.length === 0" @click="onBatchRemove">
								<el-icon><ele-Delete /></el-icon>
								批量取消
							</el-button>
							<el-button size="small" type="warning" :disabled="selectedIds.length === 0" @click="onBatchMove">
								<el-icon><ele-Switch /></el-icon>
								批量移动
							</el-button>
						</div>
					</div>

					<el-table :data="favorites" style="width: 100%" @selection-change="onSelectionChange" v-loading="loading">
						<el-table-column type="selection" width="50" />
						<el-table-column label="原图/原视频" width="120" align="center">
							<template #default="scope">
								<video
									v-if="scope.row.inputImg && (scope.row.recordType === 'VIDEO' || scope.row.recordType === 'CAMERA')"
									:src="scope.row.inputImg"
									class="record-video"
									controls
									preload="metadata"
								/>
								<el-image
									v-else-if="scope.row.inputImg && scope.row.recordType === 'IMG'"
									:src="scope.row.inputImg"
									:preview-src-list="[scope.row.inputImg]"
									fit="cover"
									class="record-image"
									preview-teleported
								/>
								<span v-else>-</span>
							</template>
						</el-table-column>
						<el-table-column label="检测结果" width="120" align="center">
							<template #default="scope">
								<video
									v-if="scope.row.outImg && (scope.row.recordType === 'VIDEO' || scope.row.recordType === 'CAMERA')"
									:src="scope.row.outImg"
									class="record-video"
									controls
									preload="metadata"
								/>
								<el-image
									v-else-if="scope.row.outImg"
									:src="scope.row.outImg"
									:preview-src-list="[scope.row.outImg]"
									fit="cover"
									class="record-image"
									preview-teleported
								/>
								<span v-else>-</span>
							</template>
						</el-table-column>
						<el-table-column prop="recordType" label="类型" width="80" align="center">
							<template #default="scope">
								<el-tag size="small" :type="getTypeTag(scope.row.recordType)">{{ getTypeLabel(scope.row.recordType) }}</el-tag>
							</template>
						</el-table-column>
						<el-table-column prop="label" label="识别结果" show-overflow-tooltip align="center">
							<template #default="scope">
								<span>{{ scope.row.label || '-' }}</span>
							</template>
						</el-table-column>
						<el-table-column prop="confidence" label="置信度" width="100" align="center">
							<template #default="scope">
								<span>{{ scope.row.confidence || '-' }}</span>
							</template>
						</el-table-column>
						<el-table-column prop="remark" label="备注" show-overflow-tooltip width="120" align="center" />
						<el-table-column prop="priority" label="优先级" width="90" align="center">
							<template #default="scope">
								<el-tag :type="getPriorityType(scope.row.priority)" size="small">
									{{ getPriorityLabel(scope.row.priority) }}
								</el-tag>
							</template>
						</el-table-column>
						<el-table-column prop="createTime" label="收藏时间" width="170" align="center">
							<template #default="scope">
								<span>{{ formatTime(scope.row.createTime) }}</span>
							</template>
						</el-table-column>
						<el-table-column label="操作" width="130" align="center" fixed="right">
							<template #default="scope">
								<el-button size="small" text type="primary" @click="onEditFavorite(scope.row)">编辑</el-button>
								<el-button size="small" text type="danger" @click="onRemoveFavorite(scope.row)">取消</el-button>
							</template>
						</el-table-column>
					</el-table>

					<el-pagination @size-change="onHandleSizeChange" @current-change="onHandleCurrentChange" class="mt15"
						:pager-count="5" :page-sizes="[10, 20, 30]" v-model:current-page="pageNum"
						background v-model:page-size="pageSize"
						layout="total, sizes, prev, pager, next, jumper" :total="total">
					</el-pagination>
				</div>
			</div>

			<!-- 公开收藏夹 -->
			<div v-if="activeTab === 'public'" class="public-favorite-layout">
				<!-- 公开收藏夹列表 -->
				<div v-if="!viewingPublicFolder" class="public-folder-grid">
					<div class="grid-header">
						<span style="font-weight: 600; font-size: 16px">其他用户的公开收藏夹</span>
						<el-input v-model="publicSearchKey" placeholder="搜索收藏夹" clearable style="width: 250px">
							<template #prefix>
								<el-icon><ele-Search /></el-icon>
							</template>
						</el-input>
					</div>
					<el-row :gutter="20" v-loading="publicLoading">
						<el-col :span="6" v-for="folder in filteredPublicFolders" :key="folder.id" style="margin-bottom: 20px">
							<el-card class="public-folder-card" shadow="hover" @click="onViewPublicFolder(folder)">
								<div class="folder-card-content">
									<div class="folder-card-icon">
										<el-icon :size="32" color="#409EFF"><ele-FolderOpened /></el-icon>
									</div>
									<h4 class="folder-card-name">{{ folder.name }}</h4>
									<p class="folder-card-desc">{{ folder.description || '暂无描述' }}</p>
									<div class="folder-card-meta">
										<span><el-icon><ele-User /></el-icon> {{ folder.username }}</span>
										<span><el-icon><ele-Star /></el-icon> {{ folder.count }} 条</span>
									</div>
								</div>
							</el-card>
						</el-col>
					</el-row>
					<el-empty v-if="!publicLoading && filteredPublicFolders.length === 0" description="暂无公开收藏夹" />
					<el-pagination v-if="publicTotal > 0" @size-change="onPublicSizeChange" @current-change="onPublicPageChange" class="mt15"
						:pager-count="5" :page-sizes="[12, 24, 36]" v-model:current-page="publicPageNum"
						background v-model:page-size="publicPageSize"
						layout="total, sizes, prev, pager, next, jumper" :total="publicTotal">
					</el-pagination>
				</div>

				<!-- 查看公开收藏夹详情 -->
				<div v-else class="public-folder-detail">
					<div class="detail-header">
						<el-button text type="primary" @click="onBackToList">
							<el-icon><ele-Back /></el-icon>
							返回列表
						</el-button>
						<div class="detail-info">
							<h3>{{ viewingPublicFolder.name }}</h3>
							<span class="detail-meta">
								<el-icon><ele-User /></el-icon> {{ viewingPublicFolder.username }}
								<span style="margin-left: 15px"><el-icon><ele-Star /></el-icon> {{ viewingPublicFolder.count }} 条收藏</span>
							</span>
						</div>
					</div>
					<p v-if="viewingPublicFolder.description" class="detail-desc">{{ viewingPublicFolder.description }}</p>

					<el-table :data="publicFavorites" style="width: 100%" v-loading="publicDetailLoading">
						<el-table-column label="原图/原视频" width="120" align="center">
							<template #default="scope">
								<video
									v-if="scope.row.inputImg && (scope.row.recordType === 'VIDEO' || scope.row.recordType === 'CAMERA')"
									:src="scope.row.inputImg"
									class="record-video"
									controls
									preload="metadata"
								/>
								<el-image
									v-else-if="scope.row.inputImg && scope.row.recordType === 'IMG'"
									:src="scope.row.inputImg"
									:preview-src-list="[scope.row.inputImg]"
									fit="cover"
									class="record-image"
									preview-teleported
								/>
								<span v-else>-</span>
							</template>
						</el-table-column>
						<el-table-column label="检测结果" width="120" align="center">
							<template #default="scope">
								<video
									v-if="scope.row.outImg && (scope.row.recordType === 'VIDEO' || scope.row.recordType === 'CAMERA')"
									:src="scope.row.outImg"
									class="record-video"
									controls
									preload="metadata"
								/>
								<el-image
									v-else-if="scope.row.outImg"
									:src="scope.row.outImg"
									:preview-src-list="[scope.row.outImg]"
									fit="cover"
									class="record-image"
									preview-teleported
								/>
								<span v-else>-</span>
							</template>
						</el-table-column>
						<el-table-column prop="recordType" label="类型" width="80" align="center">
							<template #default="scope">
								<el-tag size="small" :type="getTypeTag(scope.row.recordType)">{{ getTypeLabel(scope.row.recordType) }}</el-tag>
							</template>
						</el-table-column>
						<el-table-column prop="label" label="识别结果" show-overflow-tooltip align="center">
							<template #default="scope">
								<span>{{ scope.row.label || '-' }}</span>
							</template>
						</el-table-column>
						<el-table-column prop="confidence" label="置信度" width="100" align="center">
							<template #default="scope">
								<span>{{ scope.row.confidence || '-' }}</span>
							</template>
						</el-table-column>
						<el-table-column prop="remark" label="备注" show-overflow-tooltip width="120" align="center" />
						<el-table-column prop="priority" label="优先级" width="90" align="center">
							<template #default="scope">
								<el-tag :type="getPriorityType(scope.row.priority)" size="small">
									{{ getPriorityLabel(scope.row.priority) }}
								</el-tag>
							</template>
						</el-table-column>
						<el-table-column prop="createTime" label="收藏时间" width="170" align="center">
							<template #default="scope">
								<span>{{ formatTime(scope.row.createTime) }}</span>
							</template>
						</el-table-column>
					</el-table>

					<el-pagination @size-change="onPublicDetailSizeChange" @current-change="onPublicDetailPageChange" class="mt15"
						:pager-count="5" :page-sizes="[10, 20, 30]" v-model:current-page="publicDetailPageNum"
						background v-model:page-size="publicDetailPageSize"
						layout="total, sizes, prev, pager, next, jumper" :total="publicDetailTotal">
					</el-pagination>
				</div>
			</div>
		</div>

		<!-- 新建/编辑收藏夹对话框 -->
		<el-dialog v-model="folderDialogVisible" :title="editingFolder ? '编辑收藏夹' : '新建收藏夹'" width="400px">
			<el-form :model="folderForm" label-width="70px">
				<el-form-item label="名称">
					<el-input v-model="folderForm.name" placeholder="请输入收藏夹名称" />
				</el-form-item>
				<el-form-item label="描述">
					<el-input v-model="folderForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
				</el-form-item>
				<el-form-item label="公开">
					<el-switch v-model="folderForm.isPublic" />
					<span class="form-tip">公开后其他用户可以查看您的收藏</span>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="folderDialogVisible = false">取消</el-button>
				<el-button type="primary" @click="onSaveFolder">保存</el-button>
			</template>
		</el-dialog>

		<!-- 编辑收藏对话框 -->
		<el-dialog v-model="editDialogVisible" title="编辑收藏" width="400px">
			<el-form :model="editForm" label-width="70px">
				<el-form-item label="收藏夹">
					<el-select v-model="editForm.folderId" style="width: 100%">
						<el-option v-for="f in folders" :key="f.id" :label="f.name" :value="f.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="备注">
					<el-input v-model="editForm.remark" type="textarea" :rows="2" placeholder="添加备注信息" />
				</el-form-item>
				<el-form-item label="优先级">
					<el-select v-model="editForm.priority" style="width: 100%">
						<el-option label="普通" value="NORMAL" />
						<el-option label="重要" value="IMPORTANT" />
						<el-option label="特别重要" value="CRITICAL" />
					</el-select>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="editDialogVisible = false">取消</el-button>
				<el-button type="primary" @click="onSaveEdit">保存</el-button>
			</template>
		</el-dialog>

		<!-- 批量移动对话框 -->
		<el-dialog v-model="moveDialogVisible" title="批量移动" width="350px">
			<el-select v-model="moveTargetFolderId" placeholder="选择目标收藏夹" style="width: 100%">
				<el-option v-for="f in folders" :key="f.id" :label="f.name" :value="f.id" />
			</el-select>
			<template #footer>
				<el-button @click="moveDialogVisible = false">取消</el-button>
				<el-button type="primary" @click="onConfirmMove">确定</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts" name="favoriteIndex">
import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '/@/utils/request';

// Tab切换
const activeTab = ref('my');

// 我的收藏相关
const folders = ref<any[]>([]);
const favorites = ref<any[]>([]);
const currentFolderId = ref<number | null>(null);
const filterPriority = ref('');
const selectedIds = ref<number[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const totalCount = ref(0);
const loading = ref(false);

// 公开收藏夹相关
const publicFolders = ref<any[]>([]);
const publicPageNum = ref(1);
const publicPageSize = ref(12);
const publicTotal = ref(0);
const publicLoading = ref(false);
const publicSearchKey = ref('');
const viewingPublicFolder = ref<any>(null);
const publicFavorites = ref<any[]>([]);
const publicDetailPageNum = ref(1);
const publicDetailPageSize = ref(10);
const publicDetailTotal = ref(0);
const publicDetailLoading = ref(false);

// 收藏夹对话框
const folderDialogVisible = ref(false);
const editingFolder = ref<any>(null);
const folderForm = ref({ name: '', description: '', isPublic: false });

// 编辑收藏对话框
const editDialogVisible = ref(false);
const editForm = ref({ id: 0, folderId: 0, remark: '', priority: 'NORMAL' });

// 批量移动对话框
const moveDialogVisible = ref(false);
const moveTargetFolderId = ref<number | null>(null);

const currentFolderName = computed(() => {
	if (currentFolderId.value === null) return '全部收藏';
	const folder = folders.value.find(f => f.id === currentFolderId.value);
	return folder ? folder.name : '全部收藏';
});

// 过滤公开收藏夹
const filteredPublicFolders = computed(() => {
	if (!publicSearchKey.value) return publicFolders.value;
	return publicFolders.value.filter(f =>
		f.name.includes(publicSearchKey.value) ||
		f.username.includes(publicSearchKey.value) ||
		(f.description && f.description.includes(publicSearchKey.value))
	);
});

const getTypeLabel = (type: string) => {
	const map: Record<string, string> = { IMG: '图片', VIDEO: '视频', CAMERA: '摄像' };
	return map[type] || type;
};

const getTypeTag = (type: string) => {
	const map: Record<string, string> = { IMG: 'primary', VIDEO: 'success', CAMERA: 'warning' };
	return map[type] || 'info';
};

const getPriorityLabel = (p: string) => {
	const map: Record<string, string> = { NORMAL: '普通', IMPORTANT: '重要', CRITICAL: '特别重要' };
	return map[p] || p;
};

const getPriorityType = (p: string) => {
	const map: Record<string, string> = { NORMAL: 'info', IMPORTANT: 'warning', CRITICAL: 'danger' };
	return map[p] || 'info';
};

const formatTime = (time: string) => {
	if (!time) return '-';
	// 处理 ISO 格式时间 2026-05-23T18:47:03.000+00:00
	const date = new Date(time);
	const year = date.getFullYear();
	const month = String(date.getMonth() + 1).padStart(2, '0');
	const day = String(date.getDate()).padStart(2, '0');
	const hours = String(date.getHours()).padStart(2, '0');
	const minutes = String(date.getMinutes()).padStart(2, '0');
	const seconds = String(date.getSeconds()).padStart(2, '0');
	return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

// Tab切换
const onTabChange = (tab: string) => {
	if (tab === 'public') {
		getPublicFolders();
	}
};

// ========== 我的收藏功能 ==========

const getFolders = () => {
	request.get('/api/favorite/folders').then(res => {
		if (res.code == 0) {
			folders.value = res.data;
			// 计算总收藏数
			totalCount.value = folders.value.reduce((sum: number, f: any) => sum + (f.count || 0), 0);
		}
	});
};

const getFavorites = () => {
	loading.value = true;
	const params: any = { pageNum: pageNum.value, pageSize: pageSize.value };
	if (currentFolderId.value !== null) params.folderId = currentFolderId.value;
	if (filterPriority.value) params.priority = filterPriority.value;

	request.get('/api/favorite/list', { params }).then(res => {
		if (res.code == 0) {
			favorites.value = res.data.records || [];
			total.value = res.data.total || 0;
		}
		loading.value = false;
	});
};

const onSelectFolder = (id: number | null) => {
	currentFolderId.value = id;
	pageNum.value = 1;
	getFavorites();
};

const onSelectionChange = (rows: any[]) => {
	selectedIds.value = rows.map(r => r.id);
};

const onCreateFolder = () => {
	editingFolder.value = null;
	folderForm.value = { name: '', description: '', isPublic: false };
	folderDialogVisible.value = true;
};

const onEditFolder = (folder: any) => {
	editingFolder.value = folder;
	folderForm.value = { name: folder.name, description: folder.description, isPublic: folder.isPublic };
	folderDialogVisible.value = true;
};

const onSaveFolder = () => {
	if (!folderForm.value.name) {
		ElMessage.warning('请输入收藏夹名称');
		return;
	}
	if (editingFolder.value) {
		request.put('/api/favorite/folder', { id: editingFolder.value.id, ...folderForm.value }).then(res => {
			if (res.code == 0) {
				ElMessage.success('修改成功');
				folderDialogVisible.value = false;
				getFolders();
			}
		});
	} else {
		request.post('/api/favorite/folder', folderForm.value).then(res => {
			if (res.code == 0) {
				ElMessage.success('创建成功');
				folderDialogVisible.value = false;
				getFolders();
			}
		});
	}
};

const onDeleteFolder = (folder: any) => {
	ElMessageBox.confirm(`确定删除收藏夹"${folder.name}"？其中的收藏将移到默认收藏夹`, '提示', {
		type: 'warning',
	}).then(() => {
		request.delete('/api/favorite/folder/' + folder.id).then(res => {
			if (res.code == 0) {
				ElMessage.success('删除成功');
				if (currentFolderId.value === folder.id) currentFolderId.value = null;
				getFolders();
				getFavorites();
			}
		});
	}).catch(() => {});
};

const onEditFavorite = (row: any) => {
	editForm.value = { id: row.id, folderId: row.folderId, remark: row.remark || '', priority: row.priority || 'NORMAL' };
	editDialogVisible.value = true;
};

const onSaveEdit = () => {
	request.put('/api/favorite', editForm.value).then(res => {
		if (res.code == 0) {
			ElMessage.success('修改成功');
			editDialogVisible.value = false;
			getFavorites();
		}
	});
};

const onRemoveFavorite = (row: any) => {
	ElMessageBox.confirm('确定取消收藏？', '提示', { type: 'warning' }).then(() => {
		request.delete('/api/favorite/' + row.id).then(res => {
			if (res.code == 0) {
				ElMessage.success('已取消收藏');
				getFavorites();
				getFolders();
			}
		});
	}).catch(() => {});
};

const onBatchRemove = () => {
	ElMessageBox.confirm(`确定取消选中的 ${selectedIds.value.length} 条收藏？`, '提示', { type: 'warning' }).then(() => {
		request.post('/api/favorite/batchRemove', selectedIds.value).then(res => {
			if (res.code == 0) {
				ElMessage.success('批量取消成功');
				getFavorites();
				getFolders();
			}
		});
	}).catch(() => {});
};

const onBatchMove = () => {
	moveTargetFolderId.value = null;
	moveDialogVisible.value = true;
};

const onConfirmMove = () => {
	if (!moveTargetFolderId.value) {
		ElMessage.warning('请选择目标收藏夹');
		return;
	}
	request.post('/api/favorite/batchMove', { ids: selectedIds.value, targetFolderId: moveTargetFolderId.value }).then(res => {
		if (res.code == 0) {
			ElMessage.success('批量移动成功');
			moveDialogVisible.value = false;
			getFavorites();
			getFolders();
		}
	});
};

const onHandleSizeChange = (val: number) => {
	pageSize.value = val;
	getFavorites();
};

const onHandleCurrentChange = (val: number) => {
	pageNum.value = val;
	getFavorites();
};

// ========== 公开收藏夹功能 ==========

const getPublicFolders = () => {
	publicLoading.value = true;
	const params: any = {
		pageNum: publicPageNum.value,
		pageSize: publicPageSize.value,
	};
	request.get('/api/favorite/public/folders', { params }).then(res => {
		if (res.code == 0) {
			publicFolders.value = res.data.records || res.data || [];
			publicTotal.value = res.data.total || publicFolders.value.length;
		}
		publicLoading.value = false;
	}).catch(() => {
		publicLoading.value = false;
	});
};

const onViewPublicFolder = (folder: any) => {
	viewingPublicFolder.value = folder;
	publicDetailPageNum.value = 1;
	getPublicFolderDetail();
};

const onBackToList = () => {
	viewingPublicFolder.value = null;
	publicFavorites.value = [];
};

const getPublicFolderDetail = () => {
	if (!viewingPublicFolder.value) return;
	publicDetailLoading.value = true;
	const params: any = {
		pageNum: publicDetailPageNum.value,
		pageSize: publicDetailPageSize.value,
	};
	request.get(`/api/favorite/public/folder/${viewingPublicFolder.value.id}`, { params }).then(res => {
		if (res.code == 0) {
			publicFavorites.value = res.data.records || [];
			publicDetailTotal.value = res.data.total || 0;
		}
		publicDetailLoading.value = false;
	}).catch(() => {
		publicDetailLoading.value = false;
	});
};

const onPublicSizeChange = (val: number) => {
	publicPageSize.value = val;
	getPublicFolders();
};

const onPublicPageChange = (val: number) => {
	publicPageNum.value = val;
	getPublicFolders();
};

const onPublicDetailSizeChange = (val: number) => {
	publicDetailPageSize.value = val;
	getPublicFolderDetail();
};

const onPublicDetailPageChange = (val: number) => {
	publicDetailPageNum.value = val;
	getPublicFolderDetail();
};

onMounted(() => {
	getFolders();
	getFavorites();
});
</script>

<style scoped lang="scss">
.favorite-container {
	height: 100%;
	padding: 15px;
}

.favorite-wrapper {
	height: 100%;
	background: #fff;
	border-radius: 8px;
	display: flex;
	flex-direction: column;
}

.favorite-tabs-wrapper {
	padding: 0 20px;
	border-bottom: 1px solid #eee;
}

.favorite-tabs {
	:deep(.el-tabs__header) {
		margin: 0;
	}
	:deep(.el-tabs__nav-wrap::after) {
		background-color: transparent;
	}
	:deep(.el-tabs__item) {
		padding: 0 24px;
		font-size: 15px;
		display: inline-flex;
		align-items: center;
	}
	:deep(.el-tabs__active-bar) {
		height: 3px;
	}
}

.tab-label {
	display: inline-flex;
	align-items: center;
	gap: 6px;
}

.favorite-layout {
	display: flex;
	flex: 1;
	overflow: hidden;
}

.favorite-sidebar {
	width: 240px;
	background: #fff;
	border-right: 1px solid #eee;
	padding: 15px;
	flex-shrink: 0;
	overflow-y: auto;
}

.sidebar-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 15px;
	padding-bottom: 10px;
	border-bottom: 1px solid #eee;
}

.folder-list {
	display: flex;
	flex-direction: column;
	gap: 5px;
}

.folder-item {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 10px 12px;
	border-radius: 6px;
	cursor: pointer;
	transition: background 0.2s;
	&:hover {
		background: #f5f5f5;
	}
	&.active {
		background: #e8f5e9;
		color: #2E7D32;
	}
}

.folder-info {
	display: flex;
	align-items: center;
	gap: 8px;
}

.public-icon {
	color: #409EFF;
	font-size: 14px;
}

.folder-name {
	font-size: 14px;
}

.folder-count {
	font-size: 12px;
	color: #999;
	background: #f0f0f0;
	padding: 1px 6px;
	border-radius: 10px;
}

.folder-actions {
	display: flex;
	gap: 5px;
	i, .el-icon {
		cursor: pointer;
		color: #999;
		font-size: 14px;
		&:hover {
			color: #409EFF;
		}
	}
}

.favorite-main {
	flex: 1;
	padding: 15px;
	overflow-y: auto;
}

.main-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 15px;
	flex-wrap: wrap;
	gap: 10px;
}

.header-left {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 5px;
}

.header-right {
	display: flex;
	align-items: center;
	gap: 10px;
}

.record-image {
	width: 60px;
	height: 60px;
	border-radius: 4px;
	cursor: pointer;
}

.record-video {
	width: 100px;
	height: 60px;
	border-radius: 4px;
	object-fit: contain;
	background: #000;
}

.form-tip {
	font-size: 12px;
	color: #999;
	margin-left: 10px;
}

// 公开收藏夹样式
.public-favorite-layout {
	flex: 1;
	padding: 20px;
	overflow-y: auto;
}

.grid-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20px;
}

.public-folder-card {
	cursor: pointer;
	transition: all 0.3s;
	&:hover {
		transform: translateY(-5px);
		box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
	}
}

.folder-card-content {
	text-align: center;
	padding: 10px 0;
}

.folder-card-icon {
	margin-bottom: 12px;
}

.folder-card-name {
	margin: 0 0 8px 0;
	font-size: 15px;
	color: #333;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.folder-card-desc {
	margin: 0 0 12px 0;
	font-size: 12px;
	color: #999;
	height: 36px;
	overflow: hidden;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
}

.folder-card-meta {
	display: flex;
	justify-content: center;
	gap: 15px;
	font-size: 12px;
	color: #666;
	.el-icon {
		margin-right: 3px;
	}
}

.public-folder-detail {
	background: #fff;
}

.detail-header {
	display: flex;
	align-items: center;
	gap: 20px;
	margin-bottom: 15px;
	padding-bottom: 15px;
	border-bottom: 1px solid #eee;
}

.detail-info {
	h3 {
		margin: 0 0 5px 0;
		font-size: 18px;
	}
}

.detail-meta {
	font-size: 13px;
	color: #666;
	.el-icon {
		margin-right: 3px;
	}
}

.detail-desc {
	margin: 0 0 20px 0;
	padding: 12px 15px;
	background: #f8f9fa;
	border-radius: 6px;
	color: #666;
	font-size: 14px;
}
</style>
