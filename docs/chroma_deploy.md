## 安装chroma

### **1.安装 ChromaDB（Python 环境）**

```
pip install chromadb -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 2.**创建持久化目录**

手动建文件夹

### 3.**启动 Chroma 服务（带持久化）**

```
chroma run --host 0.0.0.0 --port 8000 --path C:\Dev\chroma_data
```

### 4.快速启动

> 可将此命令保存为 start_chroma.bat，双击即可启动。

### 5.验证chroma启动

```
http://localhost:8000/api/v2/collections/librarian-docs
```

