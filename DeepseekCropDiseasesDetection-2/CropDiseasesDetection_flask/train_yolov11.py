import os

os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"
from ultralytics import YOLO


def train_yolo11():
    # 1. 加载 YOLO11n 模型
    # 如果本地没有 yolo11n.pt，它会自动下载官方预训练权重
    model = YOLO("yolo11n.pt")
    # 2. 开启训练
    model.train(
        data="dataset/data.yaml",
        epochs=20,  # 保持 20 轮以便与 v8 公平对比
        batch=16,  # 显存稳在 16
        imgsz=640,
        device='xpu',
        amp=False,  # Intel 显卡建议关闭
        optimizer='SGD',  # 沿用 SGD
        lr0=0.01,
        workers=2,
        close_mosaic=10,  # 最后10轮关闭增强，提升精度
        name='yolo11n_crop_20e',  # 明确标记这是 yolo11
        val=True  # 既然 20 轮不长，建议开启，能实时看到 mAP 指标
    )
    return model
def resume_training():
    # 加载最后保存的权重，自动从断点继续
    model = YOLO("runs/detect/yolo11n_crop_20e2/weights/last.pt")

    # 关键：设置 resume=True
    model.train(resume=True)  # 会自动读取之前的训练状态
    return model


if __name__ == '__main__':
    # 训练
    # model = train_yolo11()
    model = resume_training()
    # 导出为 ONNX (Intel CPU/GPU 推理的最优格式)
    model.export(format='onnx', opset=11)