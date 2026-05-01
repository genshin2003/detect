<template>
    <div class="system-predict-container layout-padding" id="id" v-loading="state.loading">
        <div class="system-predict-padding layout-padding-auto layout-padding-view">
            <div class="carousel">
                <div class="section-title"><i></i><span>批量检测</span></div>
            </div>

            <!-- 控制栏 -->
            <div class="header" style="display: flex; align-items: center; gap: 20px; flex-wrap: wrap;">
                <!-- 模型选择 -->
                <div class="weight">
                    <el-select v-model="weight" placeholder="请选择模型" size="large" style="width: 200px">
                        <el-option v-for="item in state.weight_items" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                </div>

                <!-- AI助手选择 -->
                <div class="weight">
                    <el-select v-model="state.form.ai" placeholder="请选择AI助手" size="large" style="width: 200px">
                        <el-option v-for="item in state.ai_items" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                </div>

                <!-- 最小置信度 -->
                <div class="conf" style="display: flex; align-items: center; gap: 10px;">
                    <span style="font-size: 14px; color: #909399; white-space: nowrap;">设置最小置信度阈值</span>
                    <el-slider v-model="conf" :format-tooltip="formatTooltip" style="width: 300px;" :min="0" :max="100" />
                </div>

                <!-- 上传文件夹按钮 -->
                <div style="display: flex; align-items: center; gap: 15px;">
                    <el-button type="primary" @click="triggerImgFolderUpload" class="predict-button">
                        上传图片文件夹
                    </el-button>
                    <input type="file"
                           ref="imgFolder"
                           style="display: none;"
                           webkitdirectory
                           directory
                           multiple
                           accept="image/*"
                           @change="handleImgFolderChange" />

                    <el-alert v-if="state.form.imgFolder.length"
                              :title="`已选择文件夹: 包含 ${state.form.imgFolder.length} 张待检测图片`"
                              type="success"
                              :closable="false"
                              show-icon
                              style="width: fit-content; padding: 5px 15px;" />
                </div>

                <!-- 开始预测 -->
                <el-button type="primary"
                           @click="startBatchPredict"
                           class="predict-button"
                           :disabled="!state.form.imgFolder.length || !weight">
                    开始预测
                </el-button>
            </div>

            <!-- 内容区域 -->
            <div style="width: 100%; height: 800px; display: flex; flex-direction: row; margin-bottom: 20px;">
                <!-- 未上传时提示 -->
                <div v-if="!state.data.length" style="width: 100%; display: flex; flex-direction: column;">
                    <el-card shadow="hover" class="card" style="text-align: center;">
                        <p>请上传待检测图片文件夹，然后点击“开始预测”</p>
                    </el-card>
                </div>

                <!-- 预测结果表格 + 详情 -->
                <div v-else style="width: 100%; display: flex; gap: 15px;">
                    <!-- 左侧表格 -->
                    <div style="width: 70%;">
                        <el-card shadow="hover" class="card">
                            <el-table :data="state.data" style="width: 100%" height="100%">
                                <el-table-column prop="index" align="center" label="序号" width="80" />
                                <el-table-column prop="label[0]" align="center" label="识别结果" />
                                <el-table-column prop="confidence[0]" align="center" label="预测概率" />
                                <el-table-column prop="allTime" align="center" label="总耗时" />
                                <el-table-column prop="startTime" align="center" label="预测时间" />
                                <el-table-column prop="outImg" label="预测图片" width="140" align="center">
                                    <template #default="scope">
                                        <img :src="scope.row.outImg" width="120" height="60" style="object-fit: cover;" />
                                    </template>
                                </el-table-column>
                                <el-table-column label="操作" align="center" width="100">
                                    <template #default="{ $index }">
                                        <el-button type="primary"
                                                   size="small"
                                                   @click="selectResult($index)"
                                                   :disabled="$index === state.selectedIndex">
                                            {{ $index === state.selectedIndex ? '已选择' : '选择' }}
                                        </el-button>
                                    </template>
                                </el-table-column>
                            </el-table>
                        </el-card>
                    </div>

                    <!-- 右侧选中结果 -->
                    <el-card class="result-section" style="width: 30%;">
                        <!-- ... 你原来的 result-content 保持不变 ... -->
                        <div class="bottom">
                            <div class="result-content">
                                <el-card shadow="hover" class="image-card">
                                    <img v-if="outputImageUrl" :src="outputImageUrl" style="width: 100%;" />
                                </el-card>
                                <el-card shadow="never" class="info-card">
                                    <!-- 你的 info-item 保持原样 -->
                                    <div class="info-item">
                                        <div class="info-label"><el-icon class="icon"><Select /></el-icon><span>识别结果</span></div>
                                        <div class="info-value highlight">{{ state.predictionResult.label || '-' }}</div>
                                    </div>
                                    <div class="info-item">
                                        <div class="info-label"><el-icon class="icon"><Opportunity /></el-icon><span>预测概率</span></div>
                                        <div class="info-value accent">{{ state.predictionResult.confidence || '-' }}</div>
                                    </div>
                                    <div class="info-item">
                                        <div class="info-label"><el-icon class="icon"><Clock /></el-icon><span>总耗时</span></div>
                                        <div class="info-value">{{ state.predictionResult.allTime || '-' }}</div>
                                    </div>
                                </el-card>
                            </div>

                            <div style="width: 100%; margin-top: 20px;">
                                <h4>详细结果</h4>
                                <el-table :data="detailedTableData" style="width: 100%">
                                    <el-table-column prop="label" label="预测结果" align="center" />
                                    <el-table-column prop="confidence" label="置信度" align="center" />
                                    <el-table-column prop="allTime" label="总用时" align="center" />
                                </el-table>
                                <div class="info-item">
                                    <div class="info-label">
                                        <el-icon class="icon"><Opportunity /></el-icon>
                                        <span>AI建议</span>
                                    </div>
                                    <div class="info-value">{{ state.predictionResult.suggestion || '-' }}</div>
                                </div>
                            </div>
                        </div>
                    </el-card>
                </div>
            </div>
        </div>
    </div>
</template>
<script setup lang="ts" name="personal">
    import { reactive, ref, onMounted, computed } from 'vue';
    import { ElMessage } from 'element-plus';
    import request from '/@/utils/request';
    import { Select, Opportunity, Clock } from '@element-plus/icons-vue';
    import { useUserInfo } from '/@/stores/userInfo';
    import { storeToRefs } from 'pinia';

    const formatDate = (date: Date) => date.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-');

    const outputImageUrl = ref('');
    const imgFolder = ref<HTMLInputElement | null>(null);
    const conf = ref(50);
    const weight = ref('');

    const formatTooltip = (val: number) => `${val / 100}`;

    const stores = useUserInfo();
    const { userInfos } = storeToRefs(stores);

    const state = reactive({
        loading: false,
        weight_items: [] as any[],          // ← 模型列表
        ai_items: [
            { value: 'DeepSeek', label: 'DeepSeek' },
            { value: 'Qwen', label: 'Qwen' },
            { value: '不使用AI', label: '不使用AI' },
        ],
        data: [] as any[],
        selectedIndex: 0,
        predictionResult: { label: '', confidence: '', allTime: '', suggestion: '', startTime: '' },
        form: {
            username: '',
            imgFolder: [] as File[],
            ai: '不使用AI',
        },
    });

    // ==================== 详细结果计算（保持不变）===================
    const detailedTableData = computed(() => {
        const selected = state.data[state.selectedIndex];
        if (!selected || !Array.isArray(selected.label)) return [];
        return selected.label.map((label: string, idx: number) => ({
            label,
            confidence: selected.confidence?.[idx] ?? '-',
            allTime: selected.allTime,
        }));
    });

    // ==================== 其他函数保持不变 ====================
    const triggerImgFolderUpload = () => imgFolder.value?.click();

    const handleImgFolderChange = (e: Event) => {
        const files = (e.target as HTMLInputElement).files;
        if (files) {
            state.form.imgFolder = Array.from(files).filter(f => f.type.startsWith('image/'));
            ElMessage.success(`已选择 ${state.form.imgFolder.length} 张图片`);
        }
    };

    const selectResult = (index: number) => {
        state.selectedIndex = index;
        const item = state.data[index];
        if (!item) return;
        state.predictionResult.label = Array.isArray(item.label) ? item.label[0] : item.label;
        state.predictionResult.confidence = Array.isArray(item.confidence) ? item.confidence[0] : item.confidence;
        state.predictionResult.allTime = item.allTime;
        state.predictionResult.suggestion = item.suggestion;
        state.predictionResult.startTime = item.startTime;
        outputImageUrl.value = item.outImg;
    };

    // ==================== 【已修复】获取模型列表 ====================
    const loadWeightItems = () => {
        request.get('/api/flask/file_names')
            .then((res) => {
                console.log('🔍 [模型列表] 后端完整返回:', res);

                // 关键修复：兼容字符串 "0" 和数字 0
                const code = String(res.code || '').trim();   // 转成字符串再判断

                if (code === '0') {
                    const rawData = res.data;
                    let parsedData;

                    try {
                        parsedData = typeof rawData === 'string' ? JSON.parse(rawData) : rawData;
                        console.log('✅ [模型列表] 解析成功:', parsedData);
                    } catch (e) {
                        console.error('❌ JSON 解析失败:', rawData);
                        ElMessage.error('模型数据格式错误（JSON解析失败）');
                        return;
                    }

                    if (parsedData && Array.isArray(parsedData.weight_items)) {
                        state.weight_items = parsedData.weight_items;
                        console.log(`✅ 成功加载 ${state.weight_items.length} 个模型`);
                        if (state.weight_items.length === 0) {
                            ElMessage.warning('Flask 返回了空模型列表');
                        }
                    } else {
                        console.error('❌ 数据中没有 weight_items 字段', parsedData);
                        ElMessage.error('模型数据格式错误，没有 weight_items 字段');
                    }
                } else {
                    // code 不为 0 的情况
                    console.error('❌ 接口返回 code != 0:', res);
                    ElMessage.error(`获取模型失败: ${res.msg || '未知错误'}`);
                }
            })
            .catch((error) => {
                console.error('❌ 请求 /api/flask/file_names 失败:', error);
                ElMessage.error('无法连接到模型列表接口，请检查 Flask 服务（端口 5000）是否启动');
            });
    };
    const startBatchPredict = async () => {
        if (!state.form.imgFolder.length) {
            return ElMessage.warning('请先选择文件夹');
        }
        if (!weight.value) {
            return ElMessage.warning('请选择模型');
        }

        state.loading = true;

        const formData = new FormData();

        // ⭐ 一次性上传所有图片
        state.form.imgFolder.forEach(file => {
            formData.append('images', file);
        });

        formData.append('username', userInfos.value.userName);
        formData.append('weight', weight.value);
        formData.append('conf', (conf.value / 100).toString());
        formData.append('ai', state.form.ai);
        try {
            const res = await request.post(
                '/api/flask/predictImgBatch',
                formData
            );

            if (res.code === 0 || res.code === '0') {
                state.data = res.data.map((item: any, index: number) => ({
                    index: index + 1,
                    ...item
                }));

                if (state.data.length > 0) {
                    selectResult(0);
                }

                ElMessage.success(`成功处理 ${state.data.length} 张图片`);
            } else {
                ElMessage.error('批量识别失败');
            }

        } catch (err) {
            console.error(err);
            ElMessage.error('请求失败');
        }

        state.loading = false;
    };
    onMounted(() => {
        loadWeightItems();   // ← 使用新的封装函数
    });

    // 可选：页面上加一个“刷新模型”按钮（推荐）
    const refreshModels = () => {
        state.weight_items = [];
        loadWeightItems();
        ElMessage.info('正在刷新模型列表...');
    };
</script>

<style scoped lang="scss">
.carousel {
    width: 100%;
    margin-bottom: 20px;

    .section-title {
        font-size: 20px;
        text-align: center;
        position: relative;
        padding: 20px 0;
        display: flex;
        justify-content: center;
        align-items: center;

        i {
            background: #4CAF50;
            height: 1px;
            width: 100%;
            position: absolute;
            top: 50%;
            z-index: 1;
        }

        span {
            background: #4CAF50;
            line-height: 40px;
            width: 120px;
            color: #fff;
            z-index: 2;
        }
    }
}

.system-predict-container {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding: 20px;

    .system-predict-padding {
        padding: 20px;
        background: #fff;
        border-radius: 8px;
        overflow-y: auto;
        flex: 1;
    }
}

.header {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 20px;
    flex-wrap: wrap;
}

.card {
    width: 100%;
    height: 100%;
    border-radius: 10px;
    display: flex;
    flex-direction: column;
    overflow-y: scroll;

    /* Ensure table fits within card */
    .el-table {
        flex: 1;
        overflow-y: auto;
    }
}

.predict-button {
    background: #4CAF50;
    border-color: #4CAF50;
    &:hover {
        background: #2E7D32;
        border-color: #2E7D32;
    }
}

.result-section {
    width: 30%;
    height: 100%;
    border-radius: 10px;
    overflow: scroll;
    display: flex;
    flex-direction: column;

    .bottom {
        flex: 1;
        display: flex;
        flex-direction: column;
        padding: 20px;
    }

    .result-content {
        display: flex;
        flex-direction: column;
        gap: 20px;
        margin-bottom: 20px;
    }
}

.image-card {
    width: 100%;
    border-radius: 8px;
    overflow: hidden;

    img {
        width: 100%;
        height: auto;
        max-height: 200px;
        object-fit: contain;
        display: block;
    }
}

.info-card {
    padding: 20px;
    border-radius: 8px;
    background: #f8f9fa;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.info-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #eee;

    &:last-child {
        border-bottom: none;
    }

    .info-label {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #606266;
        font-size: 14px;

        .icon {
            font-size: 16px;
            color: #409eff;
        }
    }

    .info-value {
        font-size: 16px;
        font-weight: 500;
        color: #303133;
        text-align: right;

        &.highlight {
            color: #67c23a;
        }

        &.accent {
            color: #e6a23c;
        }
    }
}

/* Responsive design */
@media (max-width: 1200px) {
    .system-predict-container {
        padding: 10px;
    }

    .system-predict-padding {
        padding: 10px;
    }

    .result-section {
        width: 100%;
        margin-top: 20px;
        margin-left: 0;
    }

    .card {
        width: 100%;
        margin-right: 0;
    }

    .header {
        flex-direction: column;
        align-items: flex-start;
    }
}

/* Ensure table images are properly sized */
.el-table {
    img {
        width: 120px;
        height: 60px;
        object-fit: cover;
        border-radius: 4px;
    }
}
</style>