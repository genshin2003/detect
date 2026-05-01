import json
import time
from ultralytics import YOLO
import os
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"
import torch


class ImagePredictor:
    def __init__(self, weights_path, img_path, save_path="./runs/resultBatch.jpg", conf=0.5, device='xpu'):
        # --- 修改点 1：在这里处理设备对象 ---
        if device == 'xpu' and torch.xpu.is_available():
            self.device = torch.device("xpu")
            print("✅ 预测器已绑定 Intel GPU (XPU)")
        else:
            self.device = torch.device("cpu")
            print("ℹ️ 使用 CPU 进行预测")

        self.model = YOLO(weights_path)
        # 将模型手动移动到设备上（这一步能减少后续预测时的初始化开销）
        self.model.to(self.device)

        self.conf = conf
        self.img_path = img_path
        self.save_path = save_path
        self.labels = [
            "苹果-黑腐病",
            "苹果-健康",
            "苹果-结痂",
            "甜椒-细菌性斑疹",
            "甜椒-健康",
            "木薯-褐条病",
            "木薯-细菌性枯萎病",
            "木薯-绿斑病",
            "苹果-锈病",
            "樱桃-健康",
            "樱桃-白粉病",
            "玉米-叶斑病",
            "玉米-普通锈病",
            "玉米-健康",
            "葡萄-黑腐病",
            "葡萄-埃斯卡病",
            "葡萄-健康",
            "葡萄-叶枯病",
            "木薯-健康",
            "木薯-花叶病毒",
            "玉米-大斑病",
            "柑桔-黄龙病",
            "桃子-细菌性穿孔病",
            "桃子-健康",
            "土豆-早疫病",
            "土豆-健康",
            "土豆-晚疫病",
            "水稻-褐斑病",
            "水稻-健康",
            "水稻-稻铁甲虫",
            "水稻-稻瘟病",
            "蜘蛛螨 双斑蜘蛛螨",
            "南瓜-白粉病",
            "草莓-健康",
            "草莓-叶焦病",
            "番茄-细菌性斑点",
            "番茄-早疫病",
            "番茄-晚疫病",
            "番茄-健康",
            "番茄-叶霉病",
            "番茄-斑枯病"
        ]  # 你的标签列表保持不变
        # 构造映射字典 {0: "苹果-黑腐病", 1: "苹果-健康", ...}
        self.label_map = {i: name for i, name in enumerate(self.labels)}

    def predict(self):
        """预测图像并保存带中文标签的 BBox 图片"""
        start_time = time.time()
        try:
            results = self.model.predict(
                source=self.img_path,
                conf=self.conf,
                device=self.device,
                half=False,
                save_conf=True,
                verbose=True
            )

            end_time = time.time()
            elapsed_time = end_time - start_time

            all_results = {
                'labels': [],
                'confidences': [],
                'allTime': f"{elapsed_time:.3f}秒"
            }

            if len(results) == 0:
                return {'labels': '预测失败', 'confidences': "0.00%", 'allTime': f"{elapsed_time:.3f}秒"}

            for result in results:
                # --- 关键修改点：动态注入中文标签 ---
                # 这样 result.save() 在绘图时会去这里找中文名称
                result.names = self.label_map

                # 提取数据
                confidences = result.boxes.conf if hasattr(result.boxes, 'conf') else []
                labels = result.boxes.cls if hasattr(result.boxes, 'cls') else []

                if confidences.numel() == 0 or labels.numel() == 0:
                    return {'labels': '预测失败', 'confidences': "0.00%", 'allTime': f"{elapsed_time:.3f}秒"}

                for cls_idx, conf in zip(labels, confidences):
                    all_results['labels'].append(self.labels[int(cls_idx)])
                    all_results['confidences'].append(f"{conf * 100:.2f}%")

                # --- 关键修改点：保存图片 ---
                # YOLO 会根据刚才注入的 result.names 绘制边界框 (BBox) 标签
                # 如果是 Windows 环境，Arial 字体可能无法显示中文，
                # 如果出现乱码，请改为：result.save(filename=self.save_path, font="msyh.ttc")
                result.save(filename=self.save_path)

            return all_results

        except Exception as e:
            print(f"预测异常: {e}")
            import traceback
            traceback.print_exc()
            return {'labels': '预测失败', 'confidences': "0.00%", 'allTime': "0.000秒"}


if __name__ == '__main__':
    # 初始化预测器
    predictor = ImagePredictor("../weights/best.pt", "../result.jpg", save_path="../runs/result.jpg", conf=1.0,device='xpu')

    # 执行预测
    result = predictor.predict()
    labels_str = json.dumps(result['labels'])  # 将列表转换为 JSON 格式的字符串
    confidences_str = json.dumps(result['confidences'])  # 将列表转换为 JSON 格式的字符串
    print(labels_str)
    print(confidences_str)
    print(result['allTime'])
