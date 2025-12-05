## 项目简介

这是一个基于 TCP 自实现 HTTP 协议栈的 **简易 HTTP 服务器与客户端** 项目，包含：

- **HTTP 服务器端**：监听端口、解析 HTTP 请求、路由分发、静态文件服务、用户注册/登录/鉴权、文档目录访问/上传。
- **HTTP 客户端 CLI**：基于 TCPClient 与 HTTP 协议构造请求、处理 301/302/304 等状态码、实现简单缓存与重定向、提供交互式命令行操作。
- **命令行框架**：封装通用 CLI 基类与命令解析能力，支持历史记录、帮助等。

项目核心目标是：在不依赖现成 Web 框架的前提下，**从 TCP 层手写一个可工作的 HTTP 客户端/服务器与用户/文件服务系统**。

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
│        └─ test1          
│           ├─ ever.jpg
│           └─ test.txt
└─ src
   ├─ main
   │  ├─ java
   │  │  ├─ CLI            命令行相关
   │  │  │  ├─ CLI.java                    通用 CLI 抽象基类，封装终端、历史记录、命令表、帮助命令等
   │  │  │  ├─ Command.java                单条命令的封装，负责参数解析与帮助信息输出
   │  │  │  ├─ client
   │  │  │  │  └─ HTTPClientCLI.java       HTTP 客户端交互式 CLI
   │  │  │  └─ server
   │  │  │     └─ HTTPServerCLI.java       HTTP 服务器控制 CLI（目前主要用于启动与退出）
   │  │  ├─ HTTP           HTTP 协议层与业务层
   │  │  │  ├─ client
   │  │  │  │  ├─ HTTPClient.java          面向上层的 HTTP 客户端封装，基于 TCPClient
   │  │  │  │  └─ File.java                客户端缓存文件封装，保存响应内容与时间戳（用于 If-Modified-Since / 304）
   │  │  │  ├─ message
   │  │  │  │  ├─ HTTPRequest.java         HTTP 请求报文模型及解析/序列化
   │  │  │  │  │  ├─ HTTPRequestLine       内部类：方法/路径/版本校验与解析
   │  │  │  │  │  ├─ HTTPRequestHeaders    内部类：请求头管理与格式校验
   │  │  │  │  │  └─ HTTPRequestBody       内部类：消息体封装
   │  │  │  │  ├─ HTTPResponse.java        HTTP 响应报文模型及解析/序列化
   │  │  │  │  │  ├─ HTTPStatusLine        内部类：状态行及状态码合法性校验
   │  │  │  │  │  ├─ HTTPResponseHeaders   内部类：响应头管理与格式校验
   │  │  │  │  │  └─ HTTPResponseBody      内部类：消息体封装
   │  │  │  │  └─ exception                请求/响应行、头、整体格式异常以及方法不允许等异常定义
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
   │  │  │        ├─ UserManager.java      用户注册/登录/登出、token 管理、用户目录映射与 root token 管理
   │  │  │        └─ exception             用户名/密码格式、用户不存在、密码错误等业务异常
   │  │  │           ├─ PasswordException.java
   │  │  │           ├─ PasswordFormatException.java
   │  │  │           ├─ UsernameFormatException.java
   │  │  │           └─ UserNotExistsException.java
   │  │  ├─ TCP            TCP 抽象层
   │  │  │  ├─ TCPClient.java              对底层 Socket 的封装，提供发送/接收字节流、长连接等能力
   │  │  │  └─ TCPServer.java              通用 TCP 服务器：监听端口、处理连接、将字节流交由回调处理
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
│      应用层（CLI & 业务逻辑）       │
├─────────────────────────────────────┤
│   HTTPClient / HTTPServer           │
│   (HTTP 协议实现与请求路由)         │
├─────────────────────────────────────┤
│   TCPClient / TCPServer             │
│   (TCP 连接与字节流传输)            │
├─────────────────────────────────────┤
│   Socket（Java 标准库）             │
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

- **`TCPServer`（服务器）
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

## 运行环境

- **JDK**：11 及以上（`pom.xml` 指定 `maven.compiler.source/target` 为 11）。
- **构建工具**：Maven 3.x。
- **依赖库**：
  - `commons-cli:1.4`：命令行参数解析。
  - `jline:3.25.0`：交互式终端与历史记录。
  - `org.json:20231013`：JSON 支持。
  - `org.fusesource.jansi:2.4.0`（scope=provided）：与 jline 配合，为 Windows 等终端提供 ANSI 颜色/样式支持。
  - `jna:5.13.0`：与 jline 配合，为类 Unix 系统（Linux/macOS）终端提供本地特性支持（如终端能力探测等）。

操作系统不限（Windows / Linux / macOS 皆可），示例中的路径以当前工作目录为准。

---

## 构建与运行方法

### 1. 使用 Maven 编译与打包

在项目根目录（包含 `pom.xml` 的目录）打开终端，**首先执行打包命令**：

```bash
mvn clean package
```

该命令会先清理再编译，并在 `target/` 目录下生成 `web-1.0-SNAPSHOT.jar` 以及编译好的 `classes` 等产物，供后续运行使用。

### 2. 启动 HTTP 客户端 CLI

在同一目录下，根据操作系统使用 Maven 的 `exec:java` 目标启动客户端主类 `CLI.client.HTTPClientCLI`。

- **Windows（PowerShell / CMD）**

```bash
mvn exec:java "-Dexec.mainClass=CLI.client.HTTPClientCLI"
```

- **macOS / Linux（Bash 等）**

```bash
mvn exec:java -Dexec.mainClass='CLI.client.HTTPClientCLI'
```

执行成功后，会进入交互式 HTTP 客户端：

```text
=====simple http client=====
Client>
```

### 3. 启动本地 HTTP 服务器 CLI

如果需要本地运行 HTTP 服务器，只需将 `exec.mainClass` 替换为服务器端的主类 `CLI.server.HTTPServerCLI`，分别在不同系统下执行：

- **Windows**

```bash
mvn exec:java "-Dexec.mainClass=CLI.server.HTTPServerCLI"
```

- **macOS / Linux**

```bash
mvn exec:java -Dexec.mainClass='CLI.server.HTTPServerCLI'
```

启动后，终端中会看到类似输出，说明服务器已在 **8019 端口** 监听：

```text
=====simple http server=====
server started on port: 8019
```

此时可以在另一终端窗口按前述方式启动客户端 CLI，与本地 HTTP 服务器进行交互。

### 4. 连接到已部署在远程服务器上的服务端

本项目的 HTTP 服务器已部署在远程服务器上，IP 地址为 **`140.210.142.61`**，端口为 **`8019`**。  
在本地按照第 2 节方式启动客户端 CLI 后，可以直接通过以下命令连接并操作远程服务器：

```text
Client> enter http://140.210.142.61:8019/
```

后续即可在该连接上继续使用 `enter` / `refresh` / `fetch` / `push` / `login` / `register` / `logout` 等命令，与远程服务端进行交互。

> **使用终端的建议与已知限制**  
> - **优先使用系统终端**（Windows Terminal / PowerShell / CMD / macOS Terminal / iTerm2 / Linux 终端等），才能完整体验 CLI 的全部能力（含历史记录、正常行编辑/光标行为）。  
> - IDE 相关区别：  
>   - **IDE 一键运行（Run/Debug 按钮）**：IDE 自带虚拟控制台，登录/注册不可用，历史记录不可用（上下键仅移动光标）。  
>   - **IDE 的“终端”面板 + `mvn exec:java ...`**：IDE 实现的虚拟终端，大部分功能可用，但可能出现光标渲染问题（终端能力探测被中间层截断）。  
> - 本 CLI 目前仅提供历史记录功能，没有颜色控制、没有 Tab 补全。  

> **历史记录使用提示**  
> - 历史文件：`.history/http-client-history.txt`（客户端）/ `.history/http-server-history.txt`（服务器），在系统终端中可跨会话持久。  
> - 基本浏览：直接按 ↑/↓ 逐条查看历史，回车执行。  
> - 前缀过滤：先输入前缀（如输入 `ent`），再按 ↑，只会出现以该前缀开头的历史命令（如各类 `enter ...`），便于快速定位。  
> - 适用范围：需在系统终端使用；IDE 一键运行/虚拟终端中历史功能不可用或行为异常。  

> **网络波动与 “transmission failed” 处理提示**
> - 如遇 `transmission failed`，请参阅文档中的 **“项目限制与已知问题”** 节以获取可能原因与排查建议。

---

## 命令行使用说明

下面分别介绍 **HTTP 客户端 CLI** 和 **HTTP 服务器 CLI** 的命令。

### HTTPClientCLI 命令

所有命令都支持 `-h` / `--help` 查看自身详细说明。

- **`help`**
  - **功能**：显示所有可用命令及其简要说明。
  - **用法**：`help`

- **`exit`**
  - **功能**：退出客户端程序。若当前已登录，会先自动登出并关闭与服务器的连接。
  - **用法**：`exit`

- **`enter`**
  - **功能**：访问页面或在目录中导航。支持三种使用模式：
    1. **首次连接**：`enter <url>` 建立到服务器的连接（如 `enter http://127.0.0.1:8019/`）
    2. **目录前进**：`enter -f <path>` 在当前目录下进入子路径（如当前在 `/document/`，执行 `enter -f subfolder` 会访问 `/document/subfolder`）
    3. **目录后退**：`enter -b` 回退到上一级目录
  - **选项**：
    - `-f <path>` / `--forward <path>`：相对当前路径前进到指定子路径
    - `-b` / `--back`：后退到上一级目录
    - `-r` / `--root`：以 root 权限访问（若使用此选项需有有效的 root token）
  - **约束**：
    - `-f` 和 `-b` 不能同时使用
    - 使用完整 URL 时，若未连接会自动建立连接

- **`refresh`**
  - **功能**：重新加载当前页面（重新向服务器请求当前路径）。
  - **用法**：`refresh` 或 `refresh -r`（以 root 权限刷新）
  - **选项**：
    - `-r` / `--root`：以 root 权限重新加载
  - **前置条件**：必须已建立连接且访问过至少一个页面

- **`fetch`**
  - **功能**：从服务器下载单个文件到本地缓存目录（`.cache/`）。
  - **用法**：`fetch <filename>` 或 `fetch -r <filename>`（以 root 权限下载）
  - **选项**：
    - `-r` / `--root`：以 root 权限下载
  - **限制条件**：
    - 仅在 `/document` 路径下可用，其他路径会提示不可用
    - 只能下载文件，不能是包含路径分隔符 `/` 的表达式
    - 文件保存在 `.cache/` 目录中供后续操作使用

- **`push`**
  - **功能**：从本地缓存目录（`.cache/`）上传文件到服务器的指定目录。
  - **用法**：
    - `push <filename> <remote_dir>`：上传本地文件 `.cache/<filename>` 到服务器 `当前路径/<remote_dir>` 下
    - `push -r <filename> <remote_dir>`：以 root 权限上传
  - **选项**：
    - `-r` / `--root`：以 root 权限上传
  - **使用示例**：
    - 当前在 `/document/`，执行 `push photo.jpg .` 会将 `.cache/photo.jpg` 上传到服务器的 `/document/photo.jpg`
    - 执行 `push photo.jpg subfolder` 会上传到 `/document/subfolder/photo.jpg`
  - **限制条件**：
    - 仅在 `/document` 路径下可用
    - `<remote_dir>` 只支持相对路径（如 `.`、`subfolder`、`../other` 等）
    - 上传后服务器返回该目录的最新内容

- **`login`**
  - **功能**：使用用户名和密码登录服务器，获取 token 用于身份验证。
  - **用法**：`login`
  - **交互过程**：
    - 提示输入用户名，密码不回显（隐藏显示）
    - 登录失败可重试，最多 3 次
  - **成功结果**：
    - 服务器返回 token，客户端自动保存
    - 后续访问文档空间时会自动使用该 token 进行身份验证
    - 可以访问自己的私有文档空间
  - **常见错误**：
    - 用户不存在
    - 密码错误
    - 用户已在其他客户端登录

- **`register`**
  - **功能**：注册新用户账户，同时为该用户初始化文档空间。
  - **用法**：`register`
  - **交互过程**：
    - 提示输入用户名
    - 提示输入密码（不回显）
    - 提示确认密码（必须与第一次输入相同）
    - 最多允许重试 3 次
  - **成功结果**：
    - 用户账户创建成功
    - 服务器自动为该用户创建专属的文档空间
    - 可立即使用该账户登录
  - **常见错误**：
    - 用户名已存在
    - 用户名或密码不符合格式要求
    - 两次输入的密码不一致

- **`logout`**
  - **功能**：登出当前账户，清除 token 信息。
  - **用法**：`logout`
  - **结果**：
    - token 被服务器注销
    - 客户端清空本地保存的 token
    - 之后访问需要重新登录或以 root 权限进行
    - 自动返回到 `/login` 路径

### HTTPServerCLI 命令

服务器端 CLI 较为简单，当前主要用于查看日志与退出。

- **`help`**
  - **功能**：显示所有可用命令（当前主要是 `exit` 和 `help`）。
  - **用法**：
    - `help`

- **`exit`**
  - **功能**：停止 HTTP 服务器并退出进程。
  - **用法**：
    - `exit`

---

## 服务端用户文档空间映射与 Root 权限

### 用户文档空间映射

HTTP 服务器采用 **透明的用户目录隔离机制**，为每个用户提供独立的文件存储空间，确保用户隐私和数据安全。

- **使用体验**
  - 用户注册后即拥有一个专属的文档空间
  - 登录后通过 `/document/` 访问时，会自动访问自己的专属空间
  - **用户无需了解自己目录的实际位置**，只需正常使用 `/document/` 路径即可
  - 用户只能访问自己的文件，无法访问其他用户或系统文件

- **权限隔离**
  - 不同用户的文档空间完全独立，彼此无法互见
  - 普通用户无法越权访问其他用户或系统资源
  - 系统保留 root 权限用于管理员操作和系统维护

- **文件操作**
  - 用户注册时系统自动初始化其文档空间
  - 登录后可通过以下操作管理自己的文件：
    - 浏览文档空间中的文件和文件夹结构
    - 上传新文件到文档空间（`push` 命令）
    - 下载文件到本地（`fetch` 命令）

### Root 权限机制

Root 权限是一种特殊的系统级权限，用于绕过普通用户的访问限制。

- **用途**
  - 允许具有 root 权限的客户端访问所有用户的文档空间
  - 用于系统管理、文件维护、备份等特殊操作
  - 通常由系统管理员或具有特殊身份的客户端使用

- **Root Token**
  - 系统维护一个特殊的 `root token`，与普通用户 token 独立存储和管理
  - 仅有授权的管理员或系统配置中预设的 root token 可用
  - 需通过 `UserManager` 中的 `root token` 管理机制获取或验证

- **使用方式**
  - 在客户端命令中添加 `-r` 或 `--root` 标志
  - 支持以下命令的 root 权限版本：
    - `enter -r <path>`：以 root 权限访问任意路径
    - `refresh -r`：以 root 权限刷新当前页面
    - `fetch -r <filename>`：以 root 权限下载文件
    - `push -r <filepath> <remote path>`：以 root 权限上传文件
  - 使用 root 权限时，服务器会通过验证 root token 来授权请求


## 项目限制与已知问题

本项目为教学演示实现，以下为已知限制和使用提示：

- 接收/解析策略：本项目没有实现严格的流式分段解析（streaming）；实现上采用“先接收完整报文再解析”的方式以简化代码逻辑。该设计对小/中等大小文件适用，但对大文件传输存在局限性。

- 大文件支持说明：通过调整接收超时设置，实践中已能较稳定地传输约 3MB 左右的图片（视网络与主机性能而定）。对更大的文件不做保证，可能导致传输失败或连接中断。

- 关于 "transmission failed"：客户端显示该信息时，常见原因包括但不限于：
  1. 网络波动或超时；
  2. 待传输文件过大（超过本实现的接收能力）；
  3. 服务器端内部错误（例如文件系统异常，参见“服务器部署信息”）。

- 调试建议：
  - 遇到问题时请先查看部署主机的 `server.log`（详见“服务器部署信息”），确认是网络问题还是服务器端异常。
  - 必要时可在本地运行服务端，以验证是否为网络问题
  - 网络波动时应先用`exit`命令退出客户端再重新运行，避免传输错位(本轮接收到上一轮的报文)

