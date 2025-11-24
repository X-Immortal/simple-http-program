# HTTP 测试用例

```
java HttpClient <URL> [--method GET|POST] [--header <header>] [--data <data>] [--keep-alive]
```

## 基础状态码测试

| 测试描述 | 请求方法 | 路径 | 请求头 | 请求体 | 预期状态码 |
|---------|----------|------|--------|--------|-----------|
| 正常响应 | GET | `/` | - | - | 200 |
| 永久重定向 | GET | `/old-page` | - | - | 301 |
| 临时重定向 | GET | `/temp-redirect` | - | - | 302 |
| 未修改 | GET | `/test.txt` | `If-Modified-Since: Fri, 21 Nov 2025 13:42:22 GMT` | - | 304 |
| 页面不存在 | GET | `/test1.txt` | - | - | 404 |
| 方法不允许 | POST | `/test.txt` | - | - | 405 |
| 服务器错误 | GET | `/error` | - | - | 500 |

## 连接测试

| 测试描述 | 请求方法 | 路径 | 连接方式 |
|---------|----------|------|----------|
| 长连接 | GET | `/` | Keep-Alive |

## MIME类型测试

| 测试描述 | 请求方法 | 路径 | 预期文件类型 |
|---------|----------|------|-------------|
| 文本文件 | GET | `/test.txt` | text/plain |
| JSON数据 | GET | `/data.json` | application/json |
| 图片文件 | GET | `/ever.jpg` | image/jpeg |

## 用户功能测试

| 测试描述 | 请求方法 | 路径 | 请求体 | 预期行为 |
|---------|----------|------|--------|----------|
| 用户注册 | POST | `/register` | `username=V&password=123456` | 注册成功 |
| 重复注册 | POST | `/register` | `username=V&password=123456` | 注册失败 |
| 缺少参数 | POST | `/register` | `username=V` | 参数错误 |
| 用户登录 | POST | `/login` | `username=V&password=123456` | 登录成功 |
| 密码错误 | POST | `/login` | `username=V&password=1234567` | 登录失败 |