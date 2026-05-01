import os
from langchain.agents import create_agent
# 导入Langchain的初始化模型的函数
from langchain.chat_models import init_chat_model

base_url = os.getenv("DASHSCOPE_BASE_URL")
api_key = os.getenv("DASHSCOPE_API_KEY")
# 初始化模型
model = init_chat_model(
    model="qwen-max", # 模型名称，这里可以自定义，我们用的是阿里的qwen-max
    model_provider="openai", # 如果是Langchain不支持的模型，需要指定模型提供者（虽然我们用的是阿里，但是阿里兼容openai，所以这里用openai，就是默认采用openai的API规范）
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
    api_key="sk-1a10042344d14f89a8c730550afc0c86",
    temperature=0.7
)
agent = create_agent(model=model)

response = agent.invoke({
    "messages": [{"role": "user", "content": "月亮的首都是哪里？"}]
})

print(response)