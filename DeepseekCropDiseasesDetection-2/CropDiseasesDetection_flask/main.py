import base64
import uuid
import datetime
import numpy as np
from dotenv import load_dotenv
load_dotenv()  # 自动加载当前目录的 .env 文件
import json
import time
import os
import cv2
import jwt
from functools import wraps
from flask import Flask, Response, request, send_file, jsonify
from flask_socketio import SocketIO, emit
from ultralytics import YOLO
from utils.Fun import Fun
from utils.chat_langchain import ChatAPI

# Flask 应用设置
class VideoProcessingApp:
    def __init__(self, host='0.0.0.0', port=5000):
        """初始化 Flask 应用并设置路由"""
        self.app = Flask(__name__)
        self.socketio = SocketIO(self.app, cors_allowed_origins="*")  # 初始化 SocketIO
        self.secret_key = "crop_diseases_detection_secret_key_2026"
        self.host = host
        self.port = port
        self.fun = Fun()
        self.setup_routes()
        # 从环境变量读取密钥，避免中文占位字符串导致请求头编码错误
        self.DeepSeek = os.getenv('DASHSCOPE_API_KEY', '').strip()
        self.Qwen = os.getenv('DASHSCOPE_API_KEY', '').strip()
        self.data = {}  # 存储接收参数
        self.paths = {
            'download': './runs/video/download.mp4',
            'output': './runs/video/output.mp4',  # ✅ 最终播放用这个
            'camera_output': "./runs/video/camera_output.avi",
            'video_output': "./runs/video/camera_output.avi"
        }
        self.recording = False  # 标志位，判断是否正在录制视频
        # 全局加载（程序启动时）
        self.models = {}  # 缓存模型
        self.chat = ChatAPI(
            deepseek_api_key=self.DeepSeek,
            qwen_api_key=self.Qwen
        )

        # 建议放在类属性或全局变量中
        self.CHINESE_LABELS = {
            0: "苹果-黑腐病", 1: "苹果-健康", 2: "苹果-结痂", 3: "甜椒-细菌性斑疹", 4: "甜椒-健康",
            5: "木薯-褐条病", 6: "木薯-细菌性枯萎病", 7: "木薯-绿斑病", 8: "苹果-锈病", 9: "樱桃-健康",
            10: "樱桃-白粉病", 11: "玉米-叶斑病", 12: "玉米-普通锈病", 13: "玉米-健康", 14: "葡萄-黑腐病",
            15: "葡萄-埃斯卡病", 16: "葡萄-健康", 17: "葡萄-叶枯病", 18: "木薯-健康", 19: "木薯-花叶病毒",
            20: "玉米-大斑病", 21: "柑桔-黄龙病", 22: "桃子-细菌性穿孔病", 23: "桃子-健康", 24: "土豆-早疫病",
            25: "土豆-健康", 26: "土豆-晚疫病", 27: "水稻-褐斑病", 28: "水稻-健康", 29: "水稻-稻铁甲虫",
            30: "水稻-稻瘟病", 31: "蜘蛛螨 双斑蜘蛛螨", 32: "南瓜-白粉病", 33: "草莓-健康", 34: "草莓-叶焦病",
            35: "番茄-细菌性斑点", 36: "番茄-早疫病", 37: "番茄-晚疫病", 38: "番茄-健康", 39: "番茄-叶霉病",
            40: "番茄-斑枯病"
        }

    def setup_routes(self):
        """设置所有路由"""
        def token_required(f):
            @wraps(f)
            def decorated(*args, **kwargs):
                token = request.headers.get('Authorization') or request.args.get('token')
                if not token:
                    return jsonify({'message': 'Token is missing!', 'code': 401}), 401
                
                # 去除可能的引号或空格
                token = token.strip().strip('"').strip("'")
                
                try:
                    jwt.decode(token, self.secret_key, algorithms=["HS256"])
                except jwt.ExpiredSignatureError:
                    return jsonify({'message': 'Token has expired!', 'code': 401}), 401
                except jwt.InvalidTokenError as e:
                    print(f"Token validation failed: {str(e)}") # 打印错误日志方便调试
                    return jsonify({'message': 'Token is invalid!', 'code': 401}), 401
                return f(*args, **kwargs)
            return decorated

        self.app.add_url_rule('/file_names', 'file_names', token_required(self.file_names), methods=['GET'])
        self.app.add_url_rule('/predictImgBatch', 'predictImgBatch', token_required(self.predictImgBatch), methods=['POST'])
        self.app.add_url_rule('/predictImg', 'predictImg', token_required(self.predictImg), methods=['POST'])
        self.app.add_url_rule('/predictVideo', 'predictVideo', token_required(self.predictVideo))
        self.app.add_url_rule('/predictCamera', 'predictCamera', token_required(self.predictCamera))
        self.app.add_url_rule('/stopCamera', 'stopCamera', token_required(self.stopCamera), methods=['GET'])
        self.app.add_url_rule('/getVideo', 'getVideo', token_required(self.getVideo), methods=['GET'])
        self.app.add_url_rule('/get_image', 'get_image', token_required(self.get_image), methods=['GET'])
        # 添加 WebSocket 事件
        @self.socketio.on('connect')
        def handle_connect():
            print("WebSocket connected!")
            emit('message', {'data': 'Connected to WebSocket server!'})

        @self.socketio.on('disconnect')
        def handle_disconnect():
            print("WebSocket disconnected!")

    def run(self):
        """启动 Flask 应用"""
        self.socketio.run(self.app, host=self.host, port=self.port, allow_unsafe_werkzeug=True)


    def get_image(self):
        path = request.args.get("path")
        return send_file(path, mimetype='image/jpeg')
    def get_model(self, weight):
        model_path = f"./weights/{weight}"

        if not os.path.exists(model_path):
            raise FileNotFoundError(f"模型不存在: {model_path}")

        # 如果没加载过 → 加载
        if weight not in self.models:
            print(f"🚀 加载模型: {weight}")
            self.models[weight] = YOLO(model_path)

        return self.models[weight]

    def predictImgBatch(self):
        """批量图片预测接口"""
        files = request.files.getlist('images')
        file_names = [file.filename for file in files]
        weight = request.form.get('weight')
        conf = float(request.form.get('conf', 0.5))
        ai = request.form.get('ai', '不使用AI')
        token = request.headers.get('Authorization') or request.args.get('token')

        model = self.get_model(weight)
        model.model.names = self.CHINESE_LABELS # 注入中文标签

        imgs = []
        for file in files:
            file_bytes = np.frombuffer(file.read(), np.uint8)
            img = cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)
            imgs.append(img)

        start_time = time.time()
        start_time_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        results = model.predict(source=imgs, conf=conf)
        end_time = time.time()
        total_time = end_time - start_time
        avg_time = total_time / len(results) if len(results) > 0 else 0

        output = []
        for i, r in enumerate(results):
            labels = []
            confidences = []
            if r.boxes is not None:
                labels = [self.CHINESE_LABELS[int(cls)] for cls in r.boxes.cls]
                confidences = [f"{c * 100:.2f}%" for c in r.boxes.conf]
            
            save_name = f"batch_{uuid.uuid4().hex}.jpg"
            save_path = f"./runs/{save_name}"
            r.save(filename=save_path) # 使用原生保存
            
            uploadedUrl = self.fun.upload(save_path)
            img_bytes = cv2.imencode('.jpg', r.plot())[1].tobytes()
            img_base64 = base64.b64encode(img_bytes).decode('utf-8')

            # AI 建议逻辑 (保持简单同步处理，避免过度重构)
            suggestion = '未选择AI，无AI建议！'
            if ai in ['DeepSeek', 'Qwen'] and labels:
                list_input = self.fun.process_list(labels)
                suggestion = self.chat.generate_crop_suggestion(list_input, ai)
            elif not labels:
                suggestion = '识别失败，无法生成建议。'

            item = {
                "label": labels,
                "confidence": confidences,
                "allTime": f"{avg_time:.3f}秒",
                "startTime": start_time_str,
                "inputImg": file_names[i],
                "outImg": uploadedUrl,
                "ai": ai,
                "suggestion": suggestion,
                "outImgBase64": img_base64,
                "username": request.form.get('username'),
                "weight": weight,
                "conf": conf
            }
            output.append(item)
            
            # 批量识别的结果也入库
            self.fun.save_data(json.dumps(item), 'http://localhost:9999/imgRecords', token=token)
            
            # 清理
            self.fun.cleanup_files([save_path])

        return {"code": 0, "data": output}
    def file_names(self):
        """模型列表接口"""
        weight_items = [{'value': name, 'label': name} for name in self.fun.get_file_names("./weights")]
        return json.dumps({'weight_items': weight_items})

    def predictImg(self):
        """图片预测接口"""
        data = request.get_json()
        self.data.clear()
        self.data.update({
            "username": data['username'], "weight": data['weight'],
            "conf": data['conf'], "startTime": data['startTime'],
            "inputImg": data['inputImg'], "ai": data['ai']
        })

        # 统一使用 get_model 缓存机制
        model = self.get_model(self.data["weight"])
        start_time = time.time()
        
        # 执行推理
        results = model.predict(source=self.data["inputImg"], conf=float(self.data["conf"]))
        end_time = time.time()
        
        res = results[0]
        save_path = './runs/result.jpg'
        
        # --- 逻辑修复开始 ---
        if res.boxes is not None and len(res.boxes) > 0:
            res.save(filename=save_path) # 使用 YOLO 自带的保存方法，会自动处理中文标签映射
            uploadedUrl = self.fun.upload(save_path)
            
            self.data["status"] = 200
            self.data["message"] = "预测成功"
            self.data["outImg"] = uploadedUrl
            self.data["allTime"] = f"{end_time - start_time:.3f}秒"
            
            labels = [self.CHINESE_LABELS[int(cls)] for cls in res.boxes.cls]
            confidences = [f"{c * 100:.2f}%" for c in res.boxes.conf]
            
            self.data["confidence"] = confidences
            self.data["label"] = labels

            if self.data["ai"] in ['DeepSeek', 'Qwen']:
                ai_type = self.data["ai"]
                self.socketio.emit('message', {'data': f'已检测完成，正在生成{ai_type}AI建议！'})
                list_input = self.fun.process_list(labels)
                self.data["suggestion"] = self.chat.generate_crop_suggestion(
                    detected_labels=list_input,
                    model_type=ai_type
                )
            else:
                self.data["suggestion"] = '未选择AI，无AI建议！'
        else:
            self.data["status"] = 400
            self.data["message"] = "该图片无法识别，请重新上传！"
            self.data["suggestion"] = '识别失败，无法生成建议。'
            self.data["label"] = []
            self.data["confidence"] = []
            self.data["allTime"] = f"{end_time - start_time:.3f}秒"

        # 自动清理临时文件
        try:
            input_file = './' + self.data["inputImg"].split('/')[-1]
            self.fun.cleanup_files([input_file, save_path])
        except:
            pass

        return json.dumps(self.data, ensure_ascii=False)

    def predictVideo(self):
        """视频流处理接口"""
        # 使用局部变量避免并发请求下的数据污染
        request_data = {}
        # --- 1. 重要：在最外层作用域先初始化变量 ---
        video_writer = None
        cap = None
        real_w = 0
        real_h = 0
        web_original_path = ""
        token = request.headers.get('Authorization') or request.args.get('token')
        
        try:
            # --- A. 获取并校验参数 ---
            original_url = request.args.get('inputVideo')
            if not original_url:
                return Response("Error: 未接收到视频地址", status=400)
            
            valid_extensions = ('.mp4', '.avi', '.mov', '.mkv')
            if not original_url.lower().endswith(valid_extensions):
                return Response("Error: 不支持的文件格式", status=400)
                
            request_data.update({
                "username": request.args.get('username'),
                "weight": request.args.get('weight'),
                "conf": request.args.get('conf'),
                "startTime": request.args.get('startTime'),
                "inputVideo": original_url
            })

            # 下载并转码原视频
            self.fun.download(request_data["inputVideo"], self.paths['download'])
            
            # --- B. 深度检查：尝试用OpenCV打开 ---
            cap = cv2.VideoCapture(self.paths['download'])
            if not cap.isOpened():
                self.fun.cleanup_files([self.paths['download']])
                return Response("Error: 视频文件损坏或无法解析", status=415)

            # 检查视频是否包含有效帧
            ret, first_frame = cap.read()
            if not ret:
                cap.release()
                self.fun.cleanup_files([self.paths['download']])
                return Response("Error: 视频内容为空", status=415)
                
            # --- C. 初始化转码与写入器 ---
            web_original_path = self.paths['download'] + "_web.mp4"
            for _ in self.fun.convert_avi_to_mp4(self.paths['download'], web_original_path):
                pass

            uploaded_web_url = self.fun.upload(web_original_path)
            if uploaded_web_url:
                request_data["inputVideo"] = uploaded_web_url

            # --- 2. 获取第一帧并确定尺寸 ---
            # 重新读取第一帧（因为上面为了校验已经 read 了一次）
            cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
            ret, first_frame = cap.read()
            if not ret:
                raise ValueError("无法读取视频内容")

            real_h, real_w = first_frame.shape[:2]
            fps = int(cap.get(cv2.CAP_PROP_FPS) or 25)

            # 初始化写入器
            video_writer = cv2.VideoWriter(
                self.paths['video_output'],
                cv2.VideoWriter_fourcc(*'XVID'),
                fps,
                (real_w, real_h)
            )

            # 重置视频进度到开头
            cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
            
            # 加载模型 (修复位置：确保在 try 块内成功执行)
            model = self.get_model(request_data["weight"])
            model.model.names = self.CHINESE_LABELS

        except Exception as e:
            print(f"初始化失败: {e}")
            if cap: cap.release()
            if video_writer: video_writer.release()
            return Response(f"Error: {str(e)}", status=500)

        def generate():
            try:
                while cap and cap.isOpened():
                    ret, frame = cap.read()
                    if not ret:
                        break

                    results = model.predict(source=frame, conf=float(request_data['conf']), show=False)
                    processed_frame = results[0].plot()

                    # 尺寸安全检查
                    if processed_frame.shape[1] != real_w or processed_frame.shape[0] != real_h:
                        processed_frame = cv2.resize(processed_frame, (real_w, real_h))

                    if video_writer:
                        video_writer.write(processed_frame)

                    _, jpeg = cv2.imencode('.jpg', processed_frame)
                    yield b'--frame\r\n' b'Content-Type: image/jpeg\r\n\r\n' + jpeg.tobytes() + b'\r\n'

            except Exception as e:
                print(f"生成流时出错: {e}")

            finally:
                # --- 3. 释放资源并保存结果 ---
                self.fun.cleanup_resources(cap, video_writer)
                self.socketio.emit('message', {'data': '处理完成，正在保存！'})

                # 转码输出视频
                for progress in self.fun.convert_avi_to_mp4(
                        self.paths['video_output'],
                        self.paths['output']
                ):
                    self.socketio.emit('progress', {'data': progress})

                uploadedUrl = self.fun.upload(self.paths['output'])
                request_data["outVideo"] = uploadedUrl
                self.fun.save_data(json.dumps(request_data), 'http://localhost:9999/videoRecords', token=token)

                # 清理文件
                to_cleanup = [self.paths['download'], self.paths['output'], self.paths['video_output']]
                if web_original_path:
                    to_cleanup.append(web_original_path)
                self.fun.cleanup_files(to_cleanup)

        return Response(generate(), mimetype='multipart/x-mixed-replace; boundary=frame')
    def getVideo(self):
        video_path = request.args.get("path")

        return send_file(
            video_path,
            mimetype='video/mp4'
        )
    def predictCamera(self):
        """摄像头视频流处理接口"""
        # 1. 校验核心参数
        weight = request.args.get('weight')
        token = request.headers.get('Authorization') or request.args.get('token')
        if not weight:
            return Response("Error: 未指定模型权重", status=400)

        request_data = {
            "username": request.args.get('username'), 
            "weight": weight,
            "conf": request.args.get('conf'), 
            "startTime": request.args.get('startTime')
        }

        self.socketio.emit('message', {'data': '正在加载，请稍等！'})

        cap = cv2.VideoCapture(0)
        if not cap.isOpened():
            return Response("Error: 无法启动摄像头，请检查设备连接", status=503)
        try:
            model = self.get_model(request_data["weight"])
            model.model.names = self.CHINESE_LABELS
        except  Exception as e:
            if cap: cap.release()
            return Response(f"Error: 模型加载失败: {str(e)}", status=500)
            
        # 设置参数
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        video_writer = cv2.VideoWriter(self.paths['camera_output'], cv2.VideoWriter_fourcc(*'XVID'), 20, (640, 480))
        self.recording = True

        def generate():
            try:
                while self.recording:
                    ret, frame = cap.read()
                    if not ret:
                        break
                    results = model.predict(source=frame, imgsz=640, conf=float(request_data['conf']), show=False)
                    processed_frame = results[0].plot()
                    if self.recording and video_writer:
                        video_writer.write(processed_frame)
                    _, jpeg = cv2.imencode('.jpg', processed_frame)
                    yield b'--frame\r\n' b'Content-Type: image/jpeg\r\n\r\n' + jpeg.tobytes() + b'\r\n'
            finally:
                self.fun.cleanup_resources(cap, video_writer)
                self.socketio.emit('message', {'data': '处理完成，正在保存！'})
                for progress in self.fun.convert_avi_to_mp4(self.paths['camera_output'], self.paths['output']):
                    self.socketio.emit('progress', {'data': progress})
                uploadedUrl = self.fun.upload(self.paths['output'])
                request_data["outVideo"] = uploadedUrl
                self.fun.save_data(json.dumps(request_data), 'http://localhost:9999/cameraRecords', token=token)
                self.fun.cleanup_files([self.paths['download'], self.paths['output'], self.paths['camera_output']])

        return Response(generate(), mimetype='multipart/x-mixed-replace; boundary=frame')

    def stopCamera(self):
        """停止摄像头预测"""
        self.recording = False
        return json.dumps({"status": 200, "message": "预测成功", "code": 0})


# 启动应用
if __name__ == '__main__':
    video_app = VideoProcessingApp()
    video_app.run()
