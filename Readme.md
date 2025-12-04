## 项目简介

这是一个基于 TCP 自实现 HTTP 协议栈的 **简易 HTTP 服务器与客户端** 项目，包含：

- **HTTP 服务器端**：监听端口、解析 HTTP 请求、路由分发、静态文件服务、用户注册/登录/鉴权、文档目录访问/上传。
- **HTTP 客户端 CLI**：基于 TCPClient 与 HTTP 协议构造请求、处理 3xx/304 等状态码、实现简单缓存与重定向、提供交互式命令行操作。
- **命令行框架**：封装通用 CLI 基类与命令解析能力，支持历史记录、帮助等。

项目核心目标是：在不依赖现成 Web 框架的前提下，**从 TCP 层手写一个可工作的 HTTP 客户端/服务器与用户/文件服务系统**。

---

## 项目结构

### 顶层结构

```text
web
├─ pom.xml                 Maven 项目配置（JDK 11、依赖管理）
├─ Readme.md               项目说明（本文件）
├─ .cache                  运行时自动创建：HTTP 客户端下载文件的本地缓存目录
├─ .history                运行时自动创建：客户端/服务器 CLI 的命令历史目录
├─ .data                   运行时自动创建：用户相关数据与状态的持久化存储（例如用户信息、token 等）
├─ root                    HTTP 服务器的“站点根目录”
│  ├─ index.html           可选首页
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
│     ├─ 61dc0775
│     │  └─ test1          模拟不同用户/目录结构
│     │     ├─ ever.jpg
│     │     └─ test.txt
│     └─ test
│        └─ 14.jpg
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
   │  │  │  │  ├─ HTTPClient.java          面向上层的 HTTP 客户端封装，基于 TCPClient；负责发送请求、处理 3xx/304、维护缓存与重定向表、登录/注销/注册等高层封装
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
   │  │  │     ├─ HTTPServer.java          核心 HTTP 服务器实现，继承 TCPServer；负责解析 TCP 字节流、路由 `/`、`/register`、`/login`、`/document`、`/logout` 等，并组合 HTTPResponse，处理 200/301/302/304/400/404/405/409/500 等状态码
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

## 核心类关系（简易类图描述）

### 命令行层（CLI 相关）

命令行部分可以大致画成下面这样（`<>` 表示泛型，占位说明）：

```text
                       +----------------------+
                       |      Command         |
                       +----------------------+
                       | - usage              |
                       | - description        |
                       | - argsNum            |
                       | - options            |
                       | - handler            |
                       +----------+-----------+
                                  ^
                                  |
                 uses             |
+-----------------+     commands  |
|       CLI       |----------------------+
+-----------------+                      |
| - prompt        |                      |
| - welcome       |                      |
| - historyPath   |                      |
| - terminal      |                      |
| - reader        |                      |
| - parser        |                      |
| - commands: Map |<---------------------+
+--------+--------+
         ^
         |
         | extends
         |
  +------+----------------+       +----------------------+
  |    HTTPClientCLI      |       |    HTTPServerCLI     |
  +-----------------------+       +----------------------+
  | - client: HTTPClient  |       | - server: HTTPServer |
  | - path, baseURL       |       +----------------------+
  | - CACHE_DIR           |
  +-----------------------+
```

- **`CLI`（抽象类）**
  - 负责通用命令行框架：终端、历史记录、命令表、命令分发和 `help` 命令。
  - 持有 `commands: Map<String, Command>`，每个命令由 `Command` 表示。
- **`Command`**
  - 封装每一条子命令的 **用法 (`usage`)**、**说明 (`description`)**、**参数数目 (`argsNum`)**、**选项 (`options`)** 和 **处理函数 (`handler`)**。
- **`HTTPClientCLI` / `HTTPServerCLI`**
  - 都继承自 `CLI`，在构造时向 `commands` 注册各自的业务命令。
  - `HTTPClientCLI` 聚合 `HTTPClient`；`HTTPServerCLI` 聚合 `HTTPServer`。

### HTTP 协议与业务层

整体从 TCP 到 HTTP 再到用户/文件，可以抽象为：

```text
   +-------------------+              +-------------------+
   |     TCPClient     |<-------------|     TCPServer     |
   +-------------------+   bytes      +-------------------+
            ^                              ^
            | extends                      | extends
            |                              |
   +-------------------+          +-----------------------+
   |    HTTPClient     |          |      HTTPServer       |
   +-------------------+          +-----------------------+
   | - redirectionMap  |          | - routerMap           |
   | - cache           |          | - ROOT_PATH/...       |
   | - token           |          +-----------+-----------+
   +---------+---------+                      |
             | uses                           | uses
             v                                v
   +-------------------+          +-----------------------+
   |   HTTPRequest     |<-------->|     HTTPResponse      |
   +-------------------+          +-----------------------+
   | + RequestLine     |          | + StatusLine          |
   | + Headers         |          | + Headers             |
   | + Body            |          | + Body                |
   +-------------------+          +-----------------------+
```

以及与用户管理、工具类的关系：

```text
HTTPClient --------------------> UserManager (root token / 普通 token)
HTTPServer --------------------> UserManager (注册/登录/登出、用户目录映射)
HTTP* / HTTPServer / HTTPClient -> MIME / HTTPVersion / JSON / FileUtil / EncodingUtil ...
```

- **`HTTPClient` extends `TCPClient`**
  - 对外提供：`enter`、`push`、`login`、`register`、`logout` 等高层接口。
  - 内部组合 `HTTPRequest`/`HTTPResponse`，并通过 `TCPClient` 负责真正的发送/接收字节流。
  - 维护 `redirectionMap`、`cache`、`token` 用于处理 3xx 重定向、304 缓存以及登录态。
- **`HTTPServer` extends `TCPServer`**
  - 通过回调将 TCP 字节转换成 `HTTPRequest`，路由到 `/`、`/register`、`/login`、`/document`、`/logout` 对应的处理函数，再封装成 `HTTPResponse`。
  - 依赖 `UserManager` 进行用户注册、登录、登出和用户目录映射，依赖 `FileUtil`/`MIME`/`EncodingUtil` 等完成文件读写与 MIME 处理。
- **`HTTPRequest` / `HTTPResponse`**
  - 各自内部拆分为 **行（RequestLine/StatusLine）+ 头（Headers）+ 体（Body）**，负责报文的解析与序列化，并对方法、路径、版本、状态码等做合法性校验。

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

> **使用终端的建议**  
> 本项目的命令行使用了 jline 提供的历史记录与行编辑功能，**强烈建议在系统自带终端中运行 Maven 命令**（如 Windows Terminal / PowerShell / CMD，或 macOS Terminal / iTerm2 等），以获得完整的历史记录和快捷键支持。  
> 如果通过 IDE（一键运行 main 方法）或 IDE 自带的模拟终端运行，可能：  
> - 无法正确加载或保存历史记录（`.history` 目录下的文件不生效）；  
> - 在启动时看到一些关于终端能力探测的红色报错/警告，这些信息一般可以忽略；  
> - 某些 IDE 模拟终端对 jline/jansi/jna 的适配不完整，可能出现光标、回退键、颜色显示异常等现象。

---

## 命令行使用说明

下面分别介绍 **HTTP 客户端 CLI** 和 **HTTP 服务器 CLI** 的命令。

### HTTPClientCLI 命令

所有命令都支持 `-h` / `--help` 查看自身详细说明。

- **`help`**
  - **功能**：显示所有命令的用法和说明。
  - **用法**：
    - `help`

- **`exit`**
  - **功能**：退出客户端程序，若已登录则会先调用 `logout`，并关闭底层连接。
  - **用法**：
    - `exit`

- **`enter`**
  - **功能**：进入指定页面或在当前路径下前进/后退。
  - **用法**：
    - `enter <url>`：首次访问指定 URL，例如：`enter http://127.0.0.1:8019/`。
    - `enter -f <subpath>`：在当前路径下 **前进** 到子路径，例如当前 `/document/`，执行 `enter -f test` → `/document/test`。
    - `enter -b`：在目录层级上 **后退一级**。
  - **选项**：
    - `-f` / `--forward <subpath>`：从当前目录向下拼接子路径。
    - `-b` / `--back`：退回到上一级目录。
    - `-r` / `--root`：以 root 权限访问（使用 root token）。
  - **注意**：
    - `-f` 与 `-b` 不能同时使用。
    - 使用 URL 形式（非 `-f`/`-b`）时，会在未连接的情况下自动建立连接。

- **`refresh`**
  - **功能**：在当前路径下刷新页面（重新发起 GET 请求）。
  - **用法**：
    - `refresh`
  - **选项**：
    - `-r` / `--root`：以 root 权限刷新当前页面。
  - **前置条件**：
    - 必须已有连接且已经进入过某个路径（即 `baseURL` 和 `path` 已初始化）。

- **`fetch`**
  - **功能**：在当前 `/document/...` 页面下下载单个文件到本地缓存目录。
  - **用法**：
    - `fetch <filename>`
  - **选项**：
    - `-r` / `--root`：以 root 权限拉取文件。
  - **行为**：
    - 仅在当前路径以 `/document` 开头时有效，否则会提示 `'fetch' is invalid in this page`。
    - 仅允许单纯文件名（不能带 `/`）。
    - 成功时会将文件保存到本地 `.cache/` 目录，并提示缓存路径。

- **`push`**
  - **功能**：从本地缓存目录 `.cache/` 上传文件到服务器指定路径。
  - **用法**：
    - `push <filepath> <remote path>`
      - `<filepath>`：本地缓存中的文件名（实际上读取的是 `.cache/<filepath>`）。
      - `<remote path>`：服务器上以当前目录为基准的 **相对路径**（目录），例如 `.`、`subdir`。
  - **选项**：
    - `-r` / `--root`：以 root 权限上传。
  - **行为**：
    - 要求当前路径必须在 `/document/...` 下。
    - 最终上传路径为：`当前路径` + `<remote path>` 规范化后再拼接本地文件名。
    - 上传成功后会打印服务器返回的页面内容，并更新当前路径。

- **`login`**
  - **功能**：用户登录，获取并保存 token。
  - **用法**：
    - `login`
  - **交互流程**：
    - 控制台提示输入 `username` 和 `password`（密码不回显），最多尝试 3 次。
  - **行为**：
    - 调用 `HTTPClient.login`，若成功则从响应 JSON 中解析 `token` 并保存。
    - 失败时会根据服务器返回的 JSON 错误信息打印具体原因（如用户名/密码错误等）。

- **`register`**
  - **功能**：用户注册。
  - **用法**：
    - `register`
  - **交互流程**：
    - 依次输入 `username`、`password` 与确认密码（必须一致，最多尝试 3 次）。
  - **行为**：
    - 调用 `HTTPClient.register`，服务器创建用户并初始化该用户的文档空间。
    - 若用户已存在或格式不符合要求，会返回相应错误 JSON。

- **`logout`**
  - **功能**：用户登出。
  - **用法**：
    - `logout`
  - **行为**：
    - 调用 `HTTPClient.logout`，让服务器注销当前 token，并在客户端清空本地 token。


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

## 课程大作业选题与要求对照说明

本项目对应的选题为 **“主题1：基于Java Socket API搭建简单的HTTP客户端和服务器端程序”**，下面逐条说明项目如何满足文档中的要求，并给出示例命令。

### 1. 完全基于 Java Socket API（不使用 Netty 等框架）

- 项目中网络通信部分全部通过自定义的 `TCPClient` / `TCPServer` 实现，内部使用 JDK 原生的 `Socket` / `ServerSocket`，**未引入 Netty 或任何 Web/网络框架**。
- HTTP 层（`HTTPClient` / `HTTPServer`）都是在 TCP 字节流之上手写的解析与封装：
  - 客户端：`HTTPClient` 组装 `HTTPRequest`，通过 `TCPClient.sendMessage/receiveMessage` 发送/接收字节流，再解析为 `HTTPResponse`。
  - 服务端：`HTTPServer` 继承 `TCPServer`，在回调中把收到的字节流解析为 `HTTPRequest`，路由处理后再封装为 `HTTPResponse` 返回。

> IO 模型方面，本项目采用的是 **阻塞式 IO（BIO）**：每个连接在独立线程中以阻塞方式读写 Socket，符合题目要求的 IO 模型之一。

### 2. 实现基础的 HTTP 请求/响应功能

#### 2.1 HTTP 客户端发送请求报文、呈现响应报文

- **发送请求报文**：由 `HTTPClient` 负责构造 `HTTPRequest`（请求行 + 头 + 体），通过 TCP 发送到服务器。
- **呈现响应报文**：
  - 在客户端 CLI 中，`HTTPClientCLI` 对服务器返回的 `HTTPResponse` 做了解码并打印，**会打印响应体内容**。
  - 在服务器 CLI 中，`HTTPServerCLI` 通过回调打印收到的 `HTTPRequest` 和要发送的 `HTTPResponse`，便于观察报文内容。

**示例（连接远程服务器并查看响应）**：

```text
Client> enter http://140.210.142.61:8019/
```

执行后，客户端会输出类似：

```text
entered: http://140.210.142.61:8019/
...（welcome.txt 的内容）...
```

如果在本地同时运行 `HTTPServerCLI`，则在服务器端终端还能看到完整的请求报文和响应报文打印。

#### 2.2 客户端对 301、302、304 状态码的处理

客户端的状态码处理逻辑集中在 `HTTPClient.getResponse` 中：

- **301 / 302 重定向**：
  - 对 301（Moved Permanently）：将原始路径与新的 `Location` 记录在 `redirectionMap` 中。
  - 对 301 和 302：修改方法为 `GET`、清空请求体与 `Content-Length`，将路径改为 `Location`，**递归再次发送请求**，直到获得最终响应。
- **304 Not Modified**：
  - 若响应状态码为 304，客户端从本地缓存 `cache` 中取出之前保存的 `File` 对象，并返回其对应的旧响应，实现缓存命中。

**示例 1：目录缺少斜杠触发 301，并自动跟随重定向**

```text
Client> enter http://140.210.142.61:8019/document
```

- 服务器端 `/document` 目录访问时，如果路径末尾缺少 `/`，会返回 301，并在 `Location` 中给出 `/document/`。
- 客户端自动根据 301 响应再次发送 GET `/document/` 请求，用户看到的是**最终页面内容**而不是中间重定向细节。

**示例 2：文件再次访问触发 304 并使用缓存**

```text
Client> enter http://140.210.142.61:8019/document/test.txt
Client> enter http://140.210.142.61:8019/document/test.txt
```

- 第一次访问时，服务器返回 200 并携带 `Last-Modified`，客户端在本地 `cache` 中缓存该文件及时间戳。
- 第二次访问时，客户端在请求头中加入 `If-Modified-Since`，如果服务器返回 304，客户端从本地缓存中取出旧响应，并向用户展示相同内容。

**示例 3：302 临时重定向**

- 当在 `/document/...` 下通过 `push` 上传文件成功后，服务器会使用 302（Found）把客户端重定向回目录页面，客户端自动处理这个 302 并展示最终目录列表。

```text
Client> enter http://140.210.142.61:8019/document/
Client> push test.txt .
```

### 3. HTTP 服务器端功能与状态码支持

#### 3.1 支持 GET 和 POST 请求

- 请求方法在 `HTTPRequest.HTTPRequestLine` 中校验：
  - `supportedMethods` 只包含 `"GET"` 和 `"POST"`，若使用其他方法会抛出 `HTTPMethodNotAllowedException`。
- 在 `HTTPServer` 中：
  - `/` 路由只接受 GET（返回 `welcome.txt`）。
  - `/login` 同时支持 GET 与 POST：
    - GET 用于检查登录状态或提示需要登录。
    - POST 用于提交用户名/密码进行登录。
  - `/register`、`/logout` 仅接受 POST。
  - `/document` 路由同时支持：
    - GET：浏览目录或下载文件。
    - POST：上传文件。

#### 3.2 支持 200、301、302、304、404、405、500 等状态码

`HTTPResponse.HTTPStatusLine` 中定义并支持的状态码包括：200, 301, 302, 304, 400, 401, 404, 405, 409, 500。  
在 `HTTPServer` 中，这些状态码通过以下场景体现：

- **200 OK**：正常返回文件内容或目录列表、JSON 响应（注册/登录成功等）。
- **301 Moved Permanently**：访问目录/文件时路径结尾的 `/` 不规范（多一个或少一个）时，由 `handleMovedPermanently` 返回 301。
- **302 Found**：通过 `handleFound` 返回，例如上传文件成功后重定向到目录视图、未登录访问 `/document` 时重定向到 `/login`。
- **304 Not Modified**：客户端发送 `If-Modified-Since` 且文件未修改时，由 `handleNotModified` 返回 304。
- **404 Not Found**：访问不存在的文件或目录时，由 `handleNotFound` 返回 404，并使用 `root/msgbody/404.txt` 作为消息体。
- **405 Method Not Allowed**：对仅允许某些方法的路由使用了其他方法时，通过 `handleMethodNotAllowed` 返回 405，并在消息体中说明具体原因。
- **500 Internal Server Error**：读取文件失败、MIME 解析错误或其他未预期异常时，通过 `handleInternalServerError` 返回 500，并使用 `root/msgbody/500.txt` 作为消息体。

> 课程要求中的状态码（200、301、302、304、404、405、500）在本项目中均得到支持，另外还补充了 400、409 等状态码，用于更细致的错误表示。

#### 3.3 实现长连接

- 客户端 `HTTPClient` 继承自 `TCPClient`，在构造时建立与服务器的 TCP 连接，并在整个 CLI 生命周期中保持连接不重建：
  - 多次执行 `enter` / `fetch` / `push` / `login` / `register` / `logout` 等命令时，均复用同一 TCP 连接，体现为**长连接**。
- 服务器 `HTTPServer` 继承自 `TCPServer`，在 `run()` 方法中持续监听端口，为每个连接维护独立会话，支持同一连接上的多次请求/响应往返。

### 4. MIME 类型支持（至少三种，含一种非文本）

项目通过 `HTTP.rule.MIME` 类维护了扩展名到 MIME 类型的映射，服务器在返回文件时会根据扩展名设置 `Content-Type`。  
在 `root/document/` 中提供了多种测试文件，对应不同的 MIME 类型，例如：

- 文本类型：
  - `test.txt` → `text/plain`
  - `data.json` → `application/json`
- 非文本类型：
  - `ever.jpg` / `14.jpg` → `image/jpeg`

**示例命令**：

```text
Client> enter http://140.210.142.61:8019/document/
Client> fetch test.txt        # 下载 text/plain
Client> fetch ever.jpg        # 下载 image/jpeg
```

服务端会根据文件扩展名返回对应 MIME 类型，客户端则将内容写入本地 `.cache/` 目录。

### 5. 注册和登录功能（可配合 Postman 等工具）

根据题目要求，“数据无需持久化，存在内存中即可，只需要实现注册和登录的接口，可以使用 postman 等方法模拟请求发送，无需客户端”。  
本项目在此基础上 **实现了服务端接口 + 自定义客户端 CLI**，同时仍然可以使用 Postman / curl 等工具直接调接口。

#### 5.1 服务端接口实现

- `/register`（POST）：
  - 请求头：`Content-Type: application/json`
  - 请求体：`{"username":"...","password":"..."}`。
  - 使用 `UserManager.register` 在内存中创建用户，并为其分配私有文档目录。
- `/login`（POST）：
  - 请求头：`Content-Type: application/json`
  - 请求体：`{"username":"...","password":"..."}`。
  - 使用 `UserManager.login` 校验用户名/密码，成功时生成 token，并在 JSON 中返回：`{"success":"true","token":"..."}`。
- `/logout`（POST）：
  - 请求头中携带 `Authorization: <token>`，实现注销登录状态。
- 用户信息、登录状态、token 与用户目录映射等，均由 `UserManager` 维护在内存中；服务器重启后会清空这些状态，符合“数据无需持久化”的要求。

#### 5.2 使用 Postman / curl 调用接口示例

以远程服务器为例（`140.210.142.61:8019`）：

- **注册**：

```bash
curl -X POST "http://140.210.142.61:8019/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"alice\",\"password\":\"123456\"}"
```

（macOS/Linux 去掉 `^`，改用 `\` 续行即可）

- **登录**：

```bash
curl -X POST "http://140.210.142.61:8019/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"alice\",\"password\":\"123456\"}"
```

成功时会返回类似：

```json
{"success":"true","token":"xxxxxx"}
```

将返回的 `token` 用于后续访问 `/document` 或注销：

- **访问受保护资源**（如 `/document`）：

```bash
curl "http://140.210.142.61:8019/document/" ^
  -H "Authorization: xxxxxx"
```

- **注销**：

```bash
curl -X POST "http://140.210.142.61:8019/logout" ^
  -H "Authorization: xxxxxx"
```

#### 5.3 使用项目自带客户端体验注册/登录

在客户端 CLI 中，也可以通过交互式命令体验同样的接口：

```text
Client> enter http://140.210.142.61:8019/
Client> register
your username: alice
your password: ******
confirm your password: ******

Client> login
your username: alice
your password: ******
```

登录成功后，即可使用 `enter / fetch / push / logout` 等命令访问自己的文档目录，实现从“TCP → HTTP → 认证授权 → 文件服务”的完整链路演示。

 