import os

os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"
from ultralytics import YOLO


def train_model():
    model = YOLO("yolov8n.pt")

    model.train(
        data="dataset/data.yaml",
        epochs=20,
        batch=16,
        imgsz=640,
        device='xpu',  # 保持 xpu
        amp=False,  # Intel XPU 上 AMP 经常不稳定
        optimizer='SGD',
        workers=2,
        close_mosaic=0,
        name='v8_fast_20e',
        val=True,
        save_period=5,
        # 新增下面这行可能有帮助
        exist_ok=True
    )
    return model


if __name__ == '__main__':
    model = train_model()