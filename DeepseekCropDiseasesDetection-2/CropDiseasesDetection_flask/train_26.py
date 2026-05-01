import os
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"
import torch
from ultralytics import YOLO


def train_yolo26():
    # --- 核心修复：绕过设备校验 ---
    if torch.xpu.is_available():
        print("✅ 检测到 Intel GPU (XPU) 已就绪！")
        # 强制将目标设备设为 xpu
        target_device = torch.device("xpu")
    else:
        print("❌ 未检测到 XPU，回退到 CPU。")
        target_device = torch.device("cpu")

    # 加载模型
    model = YOLO("yolo26n.pt")

    print("🚀 正在启动 YOLO26 农作物病害检测专项训练...")

    # 训练开始
    model.train(
        data="dataset/data.yaml",
        epochs=20,
        batch=16,
        imgsz=640,
        device=target_device,
        amp=False,
        optimizer='SGD',
        lr0=0.01,
        workers=0, # XPU 环境下设为 0 可以避免多进程导致的数据搬运错误
        close_mosaic=10,
        name='yolo26n_crop_20e',
        val=True
    )
    return model

if __name__ == '__main__':
    train_yolo26()