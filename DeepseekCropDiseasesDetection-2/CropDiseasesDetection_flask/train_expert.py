import os
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"
from ultralytics import YOLO
def train_model():
    # 1. 使用绝对路径加载你之前的最佳模型
    model_path = r"F:\pycharm Profession\CropDiseasesDetection_flask\runs\detect\yolo11n_crop_20e2\weights\best.pt"

    model = YOLO(model_path)
    print("模型加载成功，开始专项微调...")

    # 2. 专项训练
    model.train(
        data="dataset/data.yaml",
        epochs=15,
        imgsz=640,
        batch=16,
        device='xpu',
        freeze=12,          # 冻结前12层
        lr0=0.001,          # 降低学习率
        mosaic=1.0,
        mixup=0.3,
        amp=False,          # 禁用 AMP 以兼容 Intel GPU
        workers=4,          # 如果报错依然存在，可以尝试调低这个数值
        name='yolo11_cassava_expert_v3'
    )

if __name__ == '__main__':
    # 只有主进程会执行这里的内容
    train_model()