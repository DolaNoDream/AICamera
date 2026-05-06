# 示例代码仅供参考，请勿在生产环境中直接使用
import os
from pathlib import Path




from alibabacloud_bailian20231229 import models as bailian_20231229_models
from alibabacloud_bailian20231229.client import Client as bailian20231229Client
from alibabacloud_tea_openapi import models as open_api_models
from alibabacloud_tea_util import models as util_models
from alibabacloud_tea_util.client import Client as UtilClient


def check_environment_variables():
    """检查并提示设置必要的环境变量"""
    required_vars = {
        'ALIBABA_CLOUD_ACCESS_KEY_ID': '阿里云访问密钥ID',
        'ALIBABA_CLOUD_ACCESS_KEY_SECRET': '阿里云访问密钥密码',
        'WORKSPACE_ID': '阿里云百炼业务空间ID'
    }
    missing_vars = []
    for var, description in required_vars.items():
        if not os.environ.get(var):
            missing_vars.append(var)
            print(f"错误：请设置 {var} 环境变量 ({description})")

    return len(missing_vars) == 0


# 创建客户端（Client）
def create_client() -> bailian20231229Client:
    """
    创建并配置客户端（Client）。

    返回:
        bailian20231229Client: 配置好的客户端（Client）。
    """
    #key_id=3205123
    #key_secret=sk-40ed2e1b81f545308b6a92574e923706
    access_key_id=""
    config = open_api_models.Config(
        access_key_id=access_key_id,
        access_key_secret=""
    )
    print(config)
    print(config.endpoint)
    print(access_key_id)
    print(config.access_key_secret)
    print(config.access_key_secret)
        # 下方接入地址以公有云的公网接入地址为例，可按需更换接入地址。
    config.endpoint = 'bailian.cn-beijing.aliyuncs.com'
    return bailian20231229Client(config)


# 检索知识库
def retrieve_index(client, workspace_id, index_id, query):
    """
    在指定的知识库中检索信息。

    参数:
        client (bailian20231229Client): 客户端（Client）。
        workspace_id (str): 业务空间ID。
        index_id (str): 知识库ID。
        query (str): 检索query。

    返回:
        阿里云百炼服务的响应。
    """
    headers = {}
    retrieve_request = bailian_20231229_models.RetrieveRequest(
        index_id=index_id,
        query=query
    )
    runtime = util_models.RuntimeOptions()
    return client.retrieve_with_options(workspace_id, retrieve_request, headers, runtime)


def main():
    """
    使用阿里云百炼服务检索知识库。

    返回:
        str or None: 如果成功，返回检索召回的文本切片；否则返回 None。
    """

    try:
        print("步骤1：创建Client")
        client = create_client()
        print("步骤2：检索知识库")
        index_id = "3a8j81v37c"  # 即 CreateIndex 接口返回的 Data.Id，您也可以在阿里云百炼控制台的知识库页面获取。
        query = "户外草地坐着的姿势"
        workspace_id = "llm-vb4fppu2ocvvl5co"

        print(workspace_id)
        print(index_id)
        
        resp = retrieve_index(client, workspace_id, index_id, query)
        result = UtilClient.to_jsonstring(resp.body)
        print(result)
    except Exception as e:
        print(f"发生错误：{e}")
        return None


if __name__ == '__main__':
    main()
