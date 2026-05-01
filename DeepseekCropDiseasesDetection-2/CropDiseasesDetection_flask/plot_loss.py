import pandas as pd
import matplotlib
matplotlib.use('Agg')   # 或者 'Qt5Agg'、'Agg'（只保存不显示）
import matplotlib.pyplot as plt

# 读取三个模型的结果
df_v8 = pd.read_csv(r"F:\pycharm Profession\CropDiseasesDetection_flask\runs\detect\v8_fast_20e\results.csv")
df_v11 = pd.read_csv(r"F:\pycharm Profession\CropDiseasesDetection_flask\runs\detect\yolo11n_crop_20e2\results.csv")
df_v26 = pd.read_csv(r"F:\pycharm Profession\CropDiseasesDetection_flask\runs\detect\yolo26n_crop_20e\results.csv")

plt.figure(figsize=(12, 10))

# --- 训练 box_loss 对比 ---
plt.subplot(2, 2, 1)
plt.plot(df_v8['epoch'], df_v8['train/box_loss'], label='YOLOv8n')
plt.plot(df_v11['epoch'], df_v11['train/box_loss'], label='YOLOv11n')
plt.plot(df_v26['epoch'], df_v26['train/box_loss'], label='YOLOv26n')
plt.xlabel('Epoch'); plt.ylabel('Box Loss')
plt.title('Train Box Loss')
plt.legend(); plt.grid(True)

# --- 验证 box_loss 对比 ---
plt.subplot(2, 2, 2)
plt.plot(df_v8['epoch'], df_v8['val/box_loss'], label='YOLOv8n')
plt.plot(df_v11['epoch'], df_v11['val/box_loss'], label='YOLOv11n')
plt.plot(df_v26['epoch'], df_v26['val/box_loss'], label='YOLOv26n')
plt.xlabel('Epoch'); plt.ylabel('Box Loss')
plt.title('Validation Box Loss')
plt.legend(); plt.grid(True)

# --- mAP@0.5 对比 ---
plt.subplot(2, 2, 3)
plt.plot(df_v8['epoch'], df_v8['metrics/mAP50(B)'], label='YOLOv8n', marker='.')
plt.plot(df_v11['epoch'], df_v11['metrics/mAP50(B)'], label='YOLOv11n', marker='.')
plt.plot(df_v26['epoch'], df_v26['metrics/mAP50(B)'], label='YOLOv26n', marker='.')
plt.xlabel('Epoch'); plt.ylabel('mAP@0.5')
plt.title('mAP@0.5 Comparison')
plt.legend(); plt.grid(True)

# --- mAP@0.5:0.95 对比 ---
plt.subplot(2, 2, 4)
plt.plot(df_v8['epoch'], df_v8['metrics/mAP50-95(B)'], label='YOLOv8n', marker='.')
plt.plot(df_v11['epoch'], df_v11['metrics/mAP50-95(B)'], label='YOLOv11n', marker='.')
plt.plot(df_v26['epoch'], df_v26['metrics/mAP50-95(B)'], label='YOLOv26n', marker='.')
plt.xlabel('Epoch'); plt.ylabel('mAP@0.5:0.95')
plt.title('mAP@0.5:0.95 Comparison')
plt.legend(); plt.grid(True)

plt.tight_layout()
plt.savefig('yolo_models_comparison.png', dpi=300)
plt.show()