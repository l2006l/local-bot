# 文件机器人（本地存储版）
## 该项目是由于本人有qq自动传文件的需求，所以利用 shiro qq机器人框架实现的机器人
# 开发环境
## JDK 17,SpringBoot3, shiro qq机器人框架 2.5.0, napcat
# 目前已实现：
### 1.qq群文件的同步（指定群号/机器人所在的所有群聊）
### 2.单个群的文件上传事件监听，同步将文件下载保存到本地
### 3.可通过qq直接搜索并下载文件
### 4.本地文件的去重功能（simple模式下载时自动去重，standard模式需要手动发送去重）
### 5.搜索结果后可以添加公告，公告写在message.txt即可，请不要写太长，以免被风控（修改后无需重新启动）
### 6.本地已有的文件可以将文件名、大小、相对路径导出到excel
### 7.由于只是我自己使用，所以并没有添加数据库，如果有需要的可以自行添加
### 8.本人只是一个代码萌新，如果有写的不好的请多包涵，我已经尽力了

## 配置文件
application.yml
```yaml
spring:
  application:
    name: local-bot

server:
  port: 8080
shiro:
  ws:
    server:
      url: /ws/shiro
      enable: true

run:
  mode: simple                # simple / standard   运行模式
  location: your/work/dir     # 工作目录，即jar包所在路径
  pwd: password               # 全部同步用到的的密码
  admin: [123456,123345]      # 管理员qq号，部分场景下用到
  groupList: [123345,123435]  # 白名单群号，即：启用功能的群聊，为空则默认全部群聊
  autoGroup: 1234567          # 自动监听群聊，有文件自动上传
  pageSize: 10                # 搜索结果分页大小

```

## 所有指令
| 指令名称 | 操作范围  |       模式        |       参数       |   指令格式    |
|:----:|:-----:|:---------------:|:--------------:|:---------:|
|  搜索  |  群聊   | simple/standrad |      关键词       |  搜索 xxx   |
|  下载  |  群聊   | simple/standard |       序号       |   下载 1    |
|  删除  |  群聊   | simple/standard |       序号       |   删除 1    |
| 上一页  |  群聊   | simple/standard |       无        |    上一页    |
| 下一页  |  群聊   | simple/standard |       无        |    下一页    |
| 文件列表 | 私聊/群聊 | simple/standard |       无        |   文件列表    |
| 同步群聊 |  私聊   | simple/standard |   机器人所在群的群号    | 同步群聊 [群号] |
| 全部同步 |  私聊   | simple/standaer | 密码（配置的run.pwd） | 全部同步 [密码] |
| 离线同步 |  私聊   |     simple      |       无        |   全部同步    |
|  去重  |  私聊   |    standard     |       无        |    去重     |
### 基础指令： 搜索 （关键词）、 下载 [序号] 、上一页 、 下一页、删除
### 文件同步： 同步群聊 [群号] 、同步所有
### 文件列表：文件列表
### simple模式： 离线同步
### standard模式： 去重

## 最终文件树
### simple模式
```
项目根目录/
├─ levels/
│  ├─ f7b8752601935ca67bed0d79028f7983/
│  │  ├─ xxx.zip
├─ tempFile/
├─ app.jar
```
### standard模式
```
项目根目录/
├─ levels/
│  ├─ 123456/
│  │  ├─ xxx.zip
├─ tempFile/
├─ app.jar
```

### simple模式的文件结构是 levels/文件md5值/文件名
### standard模式文件结构是 levels/群号/文件名

### simple模式下，文件在下载时自动进行了去重，在此基础上，不需要单独分出高性能的运算做大规模文件去重
### standard模式下，文件在下载时没有进行去重，但是这个结构更适合自己对文件进行管理，手动去重虽然会短时间进行大量IO操作，但相比之下对文件的控制更强

### 如果你的服务器采用机械硬盘，强烈建议使用simple模式，由于机械硬盘的性质，我也不知道如何解决机械硬盘的IO性能问题

## 部署
### 本项目使用 napcat 作为websocket客户端，部署请参考napcat官网，本项目采用反向连接，将napcat的websocket客户端连接配置为```ws://ip:8080/ws/shiro``` 即可