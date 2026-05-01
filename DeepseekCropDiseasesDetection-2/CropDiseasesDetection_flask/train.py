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
        device='xpu',
        amp=False,
        optimizer='SGD',
        workers=2,
        close_mosaic=0,
        name='v8_fast_20e',
        val=False,
        save_period=5
    )
    return model

if __name__ == '__main__':
    model=train_model()
    model.export(format='onnx', opset=11)
    metrics = model.val(data="dataset/data.yaml")
    print(metrics.box.map50)        # 查看平均精度