## 第19组大作业报告

选题：**基于Java Socket API搭建简单的HTTP客户端和服务器端程序**

小组构成：姚圳锴（组长）、谢易轩、殷晓瑞、张琼文

项目代码仓库：https://github.com/X-Immortal/simple-http-program.git

---

## 项目结构


```text
simple-http-program
├─ pom.xml                 Maven 项目配置（JDK 11、依赖管理）
├─ Readme.md               项目说明（本文件）
├─ .cache                  运行时自动创建：HTTP 客户端下载文件的本地缓存目录
├─ .history                运行时自动创建：客户端/服务器 CLI 的命令历史目录
├─ .data                   运行时自动创建：用户相关数据的持久化存储
├─ root                    HTTP 服务器的“站点根目录”
│  ├─ welcome.txt          访问 `/` 时返回的默认文本
│  ├─ msgbody              存放错误页面或通用消息体模板
│  │  ├─ 400.txt
│  │  ├─ 404.txt
│  │  ├─ 405.txt
│  │  └─ 500.txt
│  └─ document             用户文档根目录
│     ├─ data.json         示例 JSON 数据
│     ├─ ever.jpg          示例图片资源
│     ├─ test.txt          示例文本资源
│     ├─ test              示例文件夹资源
│     │  └─ 14.jpg
│     └─ 61dc0775          用户"user"的私有目录
│        └─ ...          
└─ src
   ├─ main
   │  ├─ java
   │  │  ├─ CLI            命令行相关
   │  │  │  ├─ CLI.java                    通用 CLI 抽象基类
   │  │  │  ├─ Command.java                单条命令的封装
   │  │  │  ├─ client
   │  │  │  │  └─ HTTPClientCLI.java       HTTP 客户端交互式 CLI
   │  │  │  └─ server
   │  │  │     └─ HTTPServerCLI.java       HTTP 服务器控制 CLI
   │  │  ├─ HTTP           HTTP 协议层与业务层
   │  │  │  ├─ client
   │  │  │  │  ├─ HTTPClient.java          面向上层的 HTTP 客户端封装
   │  │  │  │  └─ File.java                客户端缓存文件封装
   │  │  │  ├─ message
   │  │  │  │  ├─ HTTPRequest.java         HTTP 请求报文模型及解析/序列化
   │  │  │  │  │  ├─ HTTPRequestLine       内部类：方法/路径/版本校验与解析
   │  │  │  │  │  ├─ HTTPRequestHeaders    内部类：请求头管理与格式校验
   │  │  │  │  │  └─ HTTPRequestBody       内部类：消息体封装
   │  │  │  │  ├─ HTTPResponse.java        HTTP 响应报文模型及解析/序列化
   │  │  │  │  │  ├─ HTTPStatusLine        内部类：状态行及状态码合法性校验
   │  │  │  │  │  ├─ HTTPResponseHeaders   内部类：响应头管理与格式校验
   │  │  │  │  │  └─ HTTPResponseBody      内部类：消息体封装
   │  │  │  │  └─ exception                报文相关异常
   │  │  │  │     ├─ HTTPMethodNotAllowedException.java
   │  │  │  │     ├─ HTTPRequestFormatException.java
   │  │  │  │     ├─ HTTPRequestHeadersFormatException.java
   │  │  │  │     ├─ HTTPRequestLineFormatException.java
   │  │  │  │     ├─ HTTPResponseFormatException.java
   │  │  │  │     ├─ HTTPResponseHeadersFormatException.java
   │  │  │  │     └─ HTTPStatusLineFormatException.java
   │  │  │  ├─ rule
   │  │  │  │  ├─ HTTPVersion.java         支持的 HTTP 版本定义与校验
   │  │  │  │  ├─ MIME.java                扩展名与 MIME 类型映射，判断文本/二进制等
   │  │  │  │  └─ MIMETypeNotSupportedException.java  不支持的 MIME 类型异常
   │  │  │  └─ server
   │  │  │     ├─ HTTPServer.java          核心 HTTP 服务器实现，继承 TCPServer
   │  │  │     └─ user
   │  │  │        ├─ User.java             用户实体
   │  │  │        ├─ UserManager.java      用户管理类
   │  │  │        └─ exception             用户相关异常
   │  │  │           ├─ PasswordException.java
   │  │  │           ├─ PasswordFormatException.java
   │  │  │           ├─ UsernameFormatException.java
   │  │  │           └─ UserNotExistsException.java
   │  │  ├─ TCP            TCP 抽象层
   │  │  │  ├─ TCPClient.java              对底层 Socket 的封装
   │  │  │  └─ TCPServer.java              通用 TCP 服务器
   │  │  └─ utils          工具类
   │  │     ├─ EncodingUtil.java           二进制与文本（UTF-8）互转工具
   │  │     ├─ FileUtil.java               文件读写、列目录、获取扩展名/时间戳等
   │  │     ├─ JSON.java                   简易 JSON 封装与解析
   │  │     └─ URLUtil.java                路径规范化等 URL 相关工具
   │  └─ resources
   └─ test
      └─ java
```

---

## 代码设计

### 分层架构

项目采用分层设计，从下到上分别为：

```
┌─────────────────────────────────────┐
│      应用层(CLI & 业务逻辑)          │
├─────────────────────────────────────┤
│   HTTPClient / HTTPServer           │
│   (HTTP 协议实现与请求路由)           │
├─────────────────────────────────────┤
│   TCPClient / TCPServer             │
│   (TCP 连接与字节流传输)             │
├─────────────────────────────────────┤
│   Socket(Java 标准库)               │
└─────────────────────────────────────┘
```

### 命令行框架（CLI）

**设计思想**：通用的命令行处理框架，支持命令注册、解析、历史记录等功能

- **`CLI`（抽象基类）**
    - 职责：命令行交互、终端管理、命令分发
    - 核心功能：
        - 维护命令表（`commands: Map<String, Command>`）
        - 读取用户输入并解析命令
        - 提供历史记录（基于 jline3 库）
        - 自动生成帮助信息

- **`Command`（命令封装类）**
    - 职责：单个命令的定义与执行
    - 包含：用法、说明、参数数目、选项、处理函数
    - 支持自定义选项（`-h`/`--help` 自动支持）

- **`HTTPClientCLI` / `HTTPServerCLI`**（具体实现）
    - 分别继承 `CLI` 并在初始化时注册业务命令
    - `HTTPClientCLI`：提供客户端操作命令（enter、fetch、push 等）
    - `HTTPServerCLI`：提供服务器控制命令（启动、退出等）

### 网络通信层（TCP）

**设计思想**：通用的 TCP 客户端/服务器封装，支持字节流的发送与接收

- **`TCPClient`**（客户端）
    - 职责：建立连接、发送与接收字节流
    - 特点：超时控制、连接状态检测

- **`TCPServer`**（服务器）
    - 职责：监听端口、接受连接、分发字节流处理
    - 特点：线程池处理并发连接、回调模式处理字节流

### HTTP 协议层

**设计思想**：HTTP 协议的实现与业务逻辑的分离

- **`HTTPRequest` / `HTTPResponse`（报文模型）**
    - 分为三部分：**行（RequestLine/StatusLine）**、**头（Headers）**、**体（Body）**
    - 职责：报文的解析与序列化、合法性校验
    - 校验内容：方法、路径、版本、状态码等

- **`HTTPClient` extends `TCPClient`**（客户端）
    - 职责：HTTP 协议操作、请求构造、响应处理
    - 提供高层接口：`enter`、`push`、`login`、`register`、`logout`
    - 特殊处理：
        - **重定向**：自动跟随 301/302 重定向
        - **缓存**：304 Not Modified 时使用本地缓存
        - **身份认证**：维护 token 用于登录态管理

- **`HTTPServer` extends `TCPServer`**（服务器）
    - 职责：HTTP 请求路由、业务处理、响应返回
    - 路由表：将 URL 路径映射到对应的处理函数
    - 支持的路径：
        - `/`：首页
        - `/register`：用户注册
        - `/login`：用户登录
        - `/document`：文档访问与上传
        - `/logout`：用户登出

### 用户与权限管理

**`UserManager`（用户管理器）**
- 职责：用户数据持久化、身份验证、权限管理
- 核心功能：
    - 用户注册与登录
    - Token 管理（普通用户 token 与 root token 分离）
    - 用户目录映射（将用户隔离到各自的目录）
    - 权限验证

### 工具类

- **`EncodingUtil`**：字节与文本的编解码（UTF-8）
- **`FileUtil`**：文件读写、目录遍历、属性查询
- **`JSON`**：简易 JSON 解析与生成
- **`URLUtil`**：URL 路径规范化
- **`MIME`**：文件扩展名与 MIME 类型映射

---

## 各要求完成情况

以下仅针对大作业要求逐条提供分析和示例，并说明本项目的额外拓展实现。项目完整的运行方式和实现说明详见 `README.md`。

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
Client> push ./test.txt .
```
上传成功后服务器返回 302，客户端自动展示目录页。

### 3. 服务器端支持的请求方法与状态码

- 方法：只支持 GET / POST。`/` 仅 GET；`/login` GET+POST；`/register`、`/logout` 仅 POST；`/document` GET（列/取文件）+POST（上传）。
- 状态码：支持 **作业要求：200, 301, 302, 304, 404, 405, 500**；**本项目扩展：400, 409**（用于更细化的错误处理）。

#### 状态码详解与触发方式

| 状态码 | 说明 | 触发条件 | 示例                                                                                | 类型 |
|--------|------|---------|-----------------------------------------------------------------------------------|------|
| **200** | 正常返回 | 请求成功：返回文件/目录/JSON 或命令执行成功 | 访问任何有效路径、成功上传文件、登录成功                                                              | 作业要求 |
| **301** | 永久重定向 | 请求文件但路径末尾多了 `/`（如请求 `/document/test.txt/`） | `enter http://140.210.142.61:8019/document/test.txt/` 自动重定向到 `/document/test.txt` | 作业要求 |
| **302** | 临时重定向 | 1) 上传文件后重定向到目录；2) 未登录访问 `/document` 重定向到 `/login` | 1) `push ./test.txt .` 后自动跳转到当前目录；2) 未登录访问 `/document` 会重定向到 `/login`             | 作业要求 |
| **304** | 未修改 | 客户端带 `If-Modified-Since` 且文件未变更 | 同一文件连续访问两次，第二次会返回 304（客户端使用缓存）                                                    | 作业要求 |
| **400** | 请求格式错误 | 1) 不支持的 MIME 类型；2) 缺少必要参数（如注册/登录缺少 username/password）；3) JSON 格式错误 | —                                                                                 | 本项目扩展 |
| **404** | 未找到 | 文件或目录不存在 | `enter http://140.210.142.61:8019/nonexistent`                                    | 作业要求 |
| **405** | 方法不允许 | 使用不支持的 HTTP 方法（如对 `/` 发 POST、对 `/register` 发 GET） | `enter http://140.210.142.61:8019/register`                                       | 作业要求 |
| **409** | 冲突 | 1) 用户已存在（注册时）；2) 用户已登录（登录时）；3) 用户未登录（访问某些需要认证的操作） | —                                                                                 | 本项目扩展 |
| **500** | 服务器内部错误 | 服务器处理时发生异常（如文件读取失败、MIME 类型解析失败等） | 文件系统异常、权限问题等                                                                      | 作业要求 |

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