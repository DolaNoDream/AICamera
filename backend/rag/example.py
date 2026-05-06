import os
import sys
import time
from pathlib import Path

# 加载环境变量
try:
    from dotenv import load_dotenv
    ROOT_DIR = Path(__file__).resolve().parents[2]
    load_dotenv(ROOT_DIR / ".env")
except ImportError:
    pass

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from rag import RAGManager


def log_time(start_time: float, step_name: str) -> float:
    """记录并打印步骤耗时"""
    elapsed = time.time() - start_time
    print(f"  [OK] {step_name}: {elapsed:.4f} seconds")
    return elapsed


def main():
    total_start = time.time()
    print("=" * 60)
    print("RAG 示例 - 时间性能测试")
    print("=" * 60)
    
    # 获取当前脚本所在目录
    current_dir = os.path.dirname(os.path.abspath(__file__))
    # 获取backend目录
    backend_dir = os.path.dirname(current_dir)
    
    # 使用绝对路径定位JSON文件
    json_path = os.path.join(backend_dir, "ragdata", "rag_knowledge.json")
    store_path = os.path.join(current_dir, "vector_store")
    
    print("\n1. 初始化阶段")
    print("-" * 40)
    
    # 初始化RAG管理器（使用Hugging Face轻量模型）
    start = time.time()
    rag_manager = RAGManager(
        store_path=store_path,
        use_modelscope=False,
        model_name="all-MiniLM-L6-v2"
    )
    rag_manager_init_time = log_time(start, "RAG管理器初始化")
    
    print("\n2. 向量库加载/构建阶段")
    print("-" * 40)
    
    # 检查向量库是否已存在，如果不存在则构建
    start = time.time()
    vector_store_exists = rag_manager.load_vector_store()
    load_time = log_time(start, "尝试加载向量库")
    
    if not vector_store_exists:
        print("\n   向量库不存在，开始构建...")
        start = time.time()
        rag_manager.build_vector_store(json_path=json_path)
        build_time = log_time(start, "向量库构建完成")
    else:
        build_time = 0.0
    
    print("\n3. 匹配测试阶段")
    print("-" * 40)
    
    # 测试匹配功能
    test_queries = [
        "户外草地坐着的姿势",
        "室内墙面背景站立",
        "街头自拍姿势",
        "海边站立倚靠"
    ]
    
    total_match_time = 0.0
    
    for query in test_queries:
        print(f"\n   查询: {query}")
        start = time.time()
        results = rag_manager.match(query, top_k=2)
        match_time = log_time(start, f"匹配耗时")
        total_match_time += match_time
        
        for i, result in enumerate(results):
            print(f"      匹配 #{i+1} (相似度: {result['score']:.4f})")
            print(f"        文件名: {result['filename']}")
            print(f"        整体姿势: {result['data']['extracted_info']['pose_overall']}")
    
    print("\n" + "=" * 60)
    print("性能统计报告")
    print("=" * 60)
    print(f"{'步骤':<20} {'耗时(秒)':<15} {'占比':<10}")
    print("-" * 45)
    
    total_time = time.time() - total_start
    
    print(f"{'初始化':<20} {rag_manager_init_time:.4f} {'{:.1%}'.format(rag_manager_init_time/total_time):<10}")
    print(f"{'向量库加载':<20} {load_time:.4f} {'{:.1%}'.format(load_time/total_time):<10}")
    print(f"{'向量库构建':<20} {build_time:.4f} {'{:.1%}'.format(build_time/total_time):<10}")
    print(f"{'匹配查询(4次)':<20} {total_match_time:.4f} {'{:.1%}'.format(total_match_time/total_time):<10}")
    print("-" * 45)
    print(f"{'总计':<20} {total_time:.4f} {'100%':<10}")
    print("\n平均单次匹配耗时: {:.4f} 秒".format(total_match_time / len(test_queries)))


if __name__ == "__main__":
    main()
