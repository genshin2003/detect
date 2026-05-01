from dotenv import load_dotenv
load_dotenv()  # 自动加载当前目录的 .env 文件
import os
from typing import List, Dict, Optional
from langchain.chat_models import init_chat_model
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from langchain_core.callbacks import StreamingStdOutCallbackHandler


class ChatAPI:
    """使用 LangChain 封装的 ChatAPI，支持 DeepSeek 和 Qwen"""

    def __init__(self, deepseek_api_key: Optional[str] = None, qwen_api_key: Optional[str] = None):
        self.deepseek_api_key = (deepseek_api_key or os.getenv("DASHSCOPE_API_KEY", "")).strip()
        self.qwen_api_key = (qwen_api_key or os.getenv("DASHSCOPE_API_KEY", "")).strip()

        # 初始化 DeepSeek 模型
        self.deepseek_model = None
        if self.deepseek_api_key:
            try:
                self.deepseek_model = init_chat_model(
                    model="deepseek-v3.2",
                    model_provider="openai",
                    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
                    api_key=self.deepseek_api_key,
                    temperature=0.7,
                    streaming=False
                )
            except Exception as e:
                print(f"DeepSeek 模型初始化失败: {e}")

        # 初始化 Qwen 模型 (阿里 DashScope)
        self.qwen_model = None
        if self.qwen_api_key:
            try:
                # 检查 API Key 是否包含非法字符
                self.qwen_api_key.encode("latin-1")

                self.qwen_model = init_chat_model(
                    model="qwen-max",
                    model_provider="openai",
                    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
                    api_key=self.qwen_api_key,
                    temperature=0.7,
                    streaming=False
                )
            except UnicodeEncodeError:
                print("Qwen API Key 含有非法字符，请使用仅包含英文/数字/符号的密钥")
            except Exception as e:
                print(f"Qwen 模型初始化失败: {e}")

    def _convert_messages(self, messages: List[Dict]) -> List:
        """将字典格式的消息转换为 LangChain Message 对象"""
        converted = []
        for msg in messages:
            role = msg.get("role", "user")
            content = msg.get("content", "")

            if role == "system":
                converted.append(SystemMessage(content=content))
            elif role == "user":
                converted.append(HumanMessage(content=content))
            elif role == "assistant":
                converted.append(AIMessage(content=content))
            else:
                converted.append(HumanMessage(content=content))
        return converted

    def deepseek_request(self, messages: List[Dict], stream: bool = False, timeout: int = 15) -> str:
        if not self.deepseek_model:
            return "未配置 DeepSeek API Key，无法生成AI建议。"

        try:
            lc_messages = self._convert_messages(messages)
            config = {"timeout": timeout}
            if stream:
                config["callbacks"] = [StreamingStdOutCallbackHandler()]
            response = self.deepseek_model.invoke(lc_messages, config=config)
            return response.content if hasattr(response, 'content') else str(response)
        except Exception as e:
            return f"DeepSeek 请求失败：{str(e)}"

    def qwen_request(self, messages: List[Dict], timeout: int = 15) -> str:
        if not self.qwen_model:
            return "未配置 Qwen API Key，无法生成AI建议。"

        try:
            lc_messages = self._convert_messages(messages)
            response = self.qwen_model.invoke(lc_messages, config={"timeout": timeout})
            return response.content if hasattr(response, 'content') else str(response)
        except Exception as e:
            return f"Qwen 请求失败：{str(e)}"

    def generate_crop_suggestion(self, detected_labels: List[str], model_type: str = "DeepSeek", timeout: int = 15) -> str:
        """
        生成农作物病害建议的专用方法

        Args:
            detected_labels: YOLO 检测到的标签列表
            model_type: "DeepSeek" 或 "Qwen"
        """
        # 构造专业提示词
        system_prompt = """你是一位专业的农业病害防治专家。请根据用户提供的农作物病害检测结果，提供以下方面的建议：
1. 病害产生的原因分析
2. 对该作物种植的建议
3. 对该疾病的治疗措施
请用中文回答，内容要专业、实用、可操作性强。"""

        # 构建检测结果的文本描述
        labels_text = "，".join(detected_labels) if detected_labels else "未检测到明显病害"

        user_prompt = f"""我使用 YOLO 对农作物进行了检测，检测到的目标如下：{labels_text}。
请根据这些检测结果，提供实质性的种植和防治建议。"""

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt}
        ]

        if model_type == "DeepSeek":
            return self.deepseek_request(messages, timeout=timeout)
        elif model_type == "Qwen":
            return self.qwen_request(messages, timeout=timeout)
        else:
            return "未选择有效的AI模型"

# 保持与原代码兼容的别名
# ChatAPI = ChatAPILangChain

# 使用示例
if __name__ == "__main__":
    # 测试代码
    chat = ChatAPI(
        deepseek_api_key=os.getenv("DASHSCOPE_API_KEY"),
        qwen_api_key=os.getenv("DASHSCOPE_API_KEY")
    )

    # 测试生成建议
    test_labels = ["番茄早疫病", "叶片发黄"]
    print("=== DeepSeek 建议 ===")
    print(chat.generate_crop_suggestion(test_labels, "DeepSeek"))

    print("\n=== Qwen 建议 ===")
    print(chat.generate_crop_suggestion(test_labels, "Qwen"))