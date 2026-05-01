from ultralytics import YOLO
import os
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"
# 载入你训练好的 best.pt（或者 last.pt）
model = YOLO(r"F:\pycharm Profession\CropDiseasesDetection_flask\runs\detect\yolo26n_crop_20e\weights\best.pt")

# 评估验证集（关键：会生成 results.csv 和所需要的所有图像）
metrics = model.val(
    data="dataset/data.yaml",
    split='val',          # 确保使用验证集
    plots=True,           # 显式要求绘图
    save_json=False       # 不需要输出 JSON 的话可关闭
)



# 打印主要指标
print(f"mAP@0.5: {metrics.box.map50:.4f}")
print(f"mAP@0.5:0.95: {metrics.box.map:.4f}")