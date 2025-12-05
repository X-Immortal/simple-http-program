## 课程大作业选题与要求对照说明

选题：**主题1：基于Java Socket API搭建简单的HTTP客户端和服务器端程序**

本项目如何满足各条要求（含示例命令）如下。

### 1. 完全基于 Java Socket API（不使用 Netty 等框架）

- 网络通信层使用自定义的 `TCPClient` / `TCPServer`，基于 JDK 原生 `Socket` / `ServerSocket`，**未引入 Netty 或任何 Web/网络框架**。
- HTTP 层（`HTTPClient` / `HTTPServer`）在 TCP 字节流之上手写解析与封装：
  - 客户端：组装 `HTTPRequest`，通过 `TCPClient.sendMessage/receiveMessage` 发送/接收字节流，再解析为 `HTTPResponse`。
  - 服务端：继承 `TCPServer`，回调中将字节流解析为 `HTTPRequest`，路由处理后封装为 `HTTPResponse`。
- IO 模型：阻塞式 IO（BIO），每个连接独立线程，符合题目允许的 IO 模型。

### 2. 基础 HTTP 请求/响应功能

#### 2.1 客户端发送请求并呈现响应

- 发送：`HTTPClient` 构造请求行/头/体，经 TCP 发送。
- 展示：`HTTPClientCLI` 解码并打印 `HTTPResponse`（含响应体）；若在本地运行 `HTTPServerCLI`，可在服务器端打印收到/发送的完整报文。

示例：连接远程服务器并查看响应
```
Client> enter http://140.210.142.61:8019/
```
输出（示例）：
```
entered: http://140.210.142.61:8019/
...（welcome.txt 的内容）...
```

#### 2.2 客户端处理 301 / 302 / 304

- 逻辑在 `HTTPClient.getResponse`：
  - **301/302**：记录重定向（301），改用 GET、清空体并跟随 `Location` 递归重试。
  - **304**：命中本地缓存，直接返回旧响应。
  - 所有处理均自动完成，仅向用户展现最终结果

示例 1：触发 301 并自动跟随
```
Client> enter http://140.210.142.61:8019/document
```
（末尾缺 `/`，服务器返回 301 -> 客户端自动跳到 `/document/`）

示例 2：触发 304 并使用缓存
```
Client> enter http://140.210.142.61:8019/document/test.txt
Client> enter http://140.210.142.61:8019/document/test.txt
```
第二次请求带上 `If-Modified-Since`，若服务器返回 304，客户端用本地缓存。

示例 3：302 上传后重定向
```
Client> enter http://140.210.142.61:8019/document/
Client> push test.txt .
```
上传成功后服务器返回 302，客户端自动展示目录页。

### 3. 服务器端支持的请求方法与状态码

- 方法：只支持 GET / POST。`/` 仅 GET；`/login` GET+POST；`/register`、`/logout` 仅 POST；`/document` GET（列/取文件）+POST（上传）。
- 状态码：支持 **作业要求：200, 301, 302, 304, 404, 405, 500**；**本项目扩展：400, 409**（用于更细化的错误处理）。

#### 状态码详解与触发方式

| 状态码 | 说明 | 触发条件 | 示例 | 类型 |
|--------|------|---------|------|------|
| **200** | 正常返回 | 请求成功：返回文件/目录/JSON 或命令执行成功 | 访问任何有效路径、成功上传文件、登录成功 | 作业要求 |
| **301** | 永久重定向 | 请求文件但路径末尾多了 `/`（如请求 `/document/test.txt/`） | `enter http://140.210.142.61:8019/document/test.txt/` 自动重定向到 `/document/test.txt` | 作业要求 |
| **302** | 临时重定向 | 1) 上传文件后重定向到目录；2) 未登录访问 `/document` 重定向到 `/login` | 1) `push test.txt .` 后自动跳转到当前目录；2) 未登录访问 `/document` 会重定向到 `/login` | 作业要求 |
| **304** | 未修改 | 客户端带 `If-Modified-Since` 且文件未变更 | 同一文件连续访问两次，第二次会返回 304（客户端使用缓存） | 作业要求 |
| **400** | 请求格式错误 | 1) 不支持的 MIME 类型；2) 缺少必要参数（如注册/登录缺少 username/password）；3) JSON 格式错误 | — | 本项目扩展 |
| **404** | 未找到 | 文件或目录不存在 | `enter http://140.210.142.61:8019/nonexistent` | 作业要求 |
| **405** | 方法不允许 | 使用不支持的 HTTP 方法（如对 `/` 发 POST、对 `/register` 发 GET） | `enter http://140.210.142.61:8019/register` | 作业要求 |
| **409** | 冲突 | 1) 用户已存在（注册时）；2) 用户已登录（登录时）；3) 用户未登录（访问某些需要认证的操作） | — | 本项目扩展 |
| **500** | 服务器内部错误 | 服务器处理时发生异常（如文件读取失败、MIME 类型解析失败等） | 文件系统异常、权限问题等 | 作业要求 |

#### 触发示例（使用客户端 CLI）

**301 - 永久重定向：**
```bash
# 直接访问文件但末尾多了 /
Client> enter http://140.210.142.61:8019/document/test.txt/
# 服务器返回 301，Location: /document/test.txt
# 客户端自动跟随重定向
```

**302 - 临时重定向（未登录跳转登录页）：**
```bash
# 未登录直接访问 /document
Client> enter http://140.210.142.61:8019/document
# 服务器返回 302，Location: /login
# 客户端自动跳转
```

**304 - 未修改（缓存）：**
```bash
Client> enter http://140.210.142.61:8019/document/test.txt
# 第一次请求，返回 200 + 文件内容 + Last-Modified

Client> enter http://140.210.142.61:8019/document/test.txt
# 第二次请求，客户端自动带 If-Modified-Since
# 文件未改变，服务器返回 304（响应体为空）
# 客户端使用本地缓存的内容
```

**404 - 未找到：**
```bash
Client> enter http://140.210.142.61:8019/nonexistent
# 服务器返回 404，Not Found
```

**405 - 方法不允许：**
```bash
# 客户端尝试对 /register 发 GET 请求
Client> enter http://140.210.142.61:8019/register
# 服务器返回 405，Only POST method is allowed
```

**500 - 服务器内部错误：**
```bash
# 使用 user2 账户登录并访问文档（演示见服务器部署说明）
Client> enter http://140.210.142.61:8019/
Client> login
your username: user2
your password: User4321

# 登录成功后尝试访问文档目录
Client> enter http://140.210.142.61:8019/document/
# 服务器返回 500，Internal Server Error（具体原因：请参见“服务器部署信息”中 user2 的备注）
```
该示例模拟了文件系统异常场景（私有目录误删），具体的预置用户及异常情形请参阅文档末尾的“服务器部署信息”。

### 4. 长连接

- 客户端复用单条 TCP 连接完成多次 `enter/fetch/push/login/register/logout`。
- 服务器端为连接维护会话，支持同一连接内多次请求-响应往返。

### 5. MIME 类型（≥3，含非文本）

- 通过 `HTTP.rule.MIME` 做扩展名映射。示例文件：
  - 文本：`test.txt` → `text/plain`；`data.json` → `application/json`
  - 非文本：`ever.jpg` / `14.jpg` → `image/jpeg`

示例下载：
```
Client> enter http://140.210.142.61:8019/document/
Client> fetch test.txt        # text/plain
Client> fetch ever.jpg        # image/jpeg
```

### 6. 注册与登录

- 功能说明：`/register`（POST, JSON）用于创建用户并初始化文档空间；`/login`（POST, JSON）用于身份验证并返回 token；`/logout`（POST）用于注销 token。
- 作业要求：实现用户注册与登录的**内存**管理（程序运行期有效，重启后数据清空）。
- 本项目扩展：对用户数据进行了**持久化存储**（项目实现中保存于 `.data/users`，重启后仍可保留用户），此为作业外的额外功能。
- 说明：本仓库中已在服务器上预置若干用户（详见“服务器部署信息”）。有关如何在客户端 CLI 中交互式注册/登录的示例，请使用 CLI 的 `register` / `login` 命令；服务器上已预置的测试用户与特殊异常（如 user2 的私有目录缺失导致 500）请参阅“服务器部署信息”。

---

## 服务器部署信息

### 基本配置

- **小组**：Group 19
- **项目位置**：本小组用户目录下的 `simple-http-program`
- **服务器地址**：`140.210.142.61:8019`
- **服务器端口**：`8019`

### 启动与停止

**服务器运行状态**：

服务器已使用以下命令挂在后台运行：
```bash
nohup mvn exec:java -Dexec.mainClass='CLI.server.HTTPServerCLI' > server.log 2>&1 &
```
- 所有的请求/响应报文均重定向保存到项目根目录下的 `server.log` 文件
- 当报文体（body）为非文本内容时，不会打印报文体内容

**停止服务器**：
```bash
# 1. 查看服务器进程
lsof -i :8019

# 2. 查出进程 PID 后，使用 kill 杀死
kill -9 <PID>
```

### 预置用户

服务器上已注册的用户及其密码：

| 用户名 | 密码 | 备注 |
|--------|------|------|
| `user` | `User1234` | 正常用户，私有目录完整 |
| `user2` | `User4321` | 私有目录已被删除，模拟文件系统异常（访问文件时返回 500） |

### 日志查看

- **实时查看日志**：
  ```bash
  tail -f server.log
  ```
- **查看最近 N 行**：
  ```bash
  tail -n 100 server.log
  ```
- **搜索特定内容**：
  ```bash
  grep "GET\|POST" server.log
  ```