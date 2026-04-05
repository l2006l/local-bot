# 文件机器人（本地存储版）

## 该项目是由于本人有qq自动传文件的需求，所以利用 shiro qq机器人框架实现的机器人

# 开发环境

## JDK 17,SpringBoot3, shiro qq机器人框架 2.5.0, napcat， Erupt低代码框架

# 目前已实现：

- [x] 1.qq群文件的同步（指定群号/机器人所在的所有群聊）

- [x] 2.单个群的文件上传事件监听，同步将文件下载保存到本地

- [x] 3.可通过qq直接搜索并下载文件

~~4.本地文件的去重功能（simple模式下载时自动去重，standard模式需要手动发送去重）~~

- [x] 4.添加数据库存储文件信息和Erupt框架实现文件管理

- [x] 5.搜索结果后可以添加公告，公告写在message.txt即可，请不要写太长，以免被风控（修改后无需重新启动）

- [x] 6.本地已有的文件可以将文件名、大小、相对路径导出到excel

~~7.由于只是我自己使用，所以并没有添加数据库，如果有需要的可以自行添加~~

8.本人只是一个代码萌新，如果有写的不好的请多包涵，我已经尽力了

## 配置文件

application.yml

这里是完整版的，快速启动则不需要进行任何配置，可以实现一键部署

```yaml
spring:
  application:
    name: local-bot
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:./data/db;AUTO_SERVER=TRUE;MODE=MySQL;NON_KEYWORDS=VALUE
    username: sa
    password:
  jpa:
    generate-ddl: true
    database-platform: org.hibernate.dialect.H2Dialect
    database: h2
  servlet:
    multipart:
      max-file-size: -1
      max-request-size: -1
  sql:
    init:
      encoding: UTF-8
      mode: always
      data-locations: sql/init.sql

mybatis-plus:
  global-config:
    banner: false
    db-config:
      id-type: assign_id
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    lazy-loading-enabled: true

erupt-app:
  verify-code-count: 1
  reset-pwd: true
  water-mark: false
erupt:
  upload-path: ${run.upload-path:${user.dir}/upload}
  hot-build: false
  upms:
    default-account: admin
    default-password: admin
    expire-time-by-login: 24
shiro:
  ws:
    server:
      url: /ws/shiro  # 反向websocket的端点
      enable: true

server:
  tomcat:
    max-http-form-post-size: -1
    max-swallow-size: -1
  port: 8080             # 项目端口
run:
  pageSize: 20         # 搜索结果分页大小
  upload-path: ${user.dir}/upload     # 公告图片的保存位置
  napcat-web: "http://127.0.0.1:6099"     # napcat的web地址，可以将napcat的页面嵌入到项目首页
```

## 所有指令

|        指令名称        |  操作范围  |            参数             |          指令格式           | 权限需要 |
|:------------------:|:------:|:-------------------------:|:-----------------------:|:----:|
|         搜索         |   群聊   |            关键词            |         搜索 xxx          | 白名单群 |
|         下载         |   群聊   |            序号             |          下载 1           | 白名单群 |
|         删除         |   群聊   |            序号             |          删除 1           | 白名单群 |
|        上一页         |   群聊   |             无             |           上一页           | 白名单群 |
|        下一页         |   群聊   |             无             |           下一页           | 白名单群 |
|        文件列表        | 私聊/群聊  |             无             |          文件列表           |  无   |
|        同步群聊        |   私聊   |         机器人所在群的群号         |        同步群聊 [群号]        |  超管  |
|        全部同步        |   私聊   | ~~密码（配置的run.pwd）~~ <br/>无 | ~~全部同步 [密码]~~ <br/>全部同步 |  超管  |
| ~~离线同步~~ <br/>文件整合 | 私聊/群聊  |             无             |   ~~离线同步~~ <br/>文件整合    |  超管  |
|       ~~去重~~       | ~~私聊~~ |           ~~无~~           |         ~~去重~~          |      |

## 其他

- 项目利用erupt生成了一个文件管理后台，项目启动后可以访问 `http://localhost:8080` 进入后台配置用户白名单等内容
- 可以在后台配置配置公告（可加图片），公告会附带在几乎所有机器人回复的结尾处，可以用来做通知或者机器人的图文教程等内容
- 可以在后台创建新用户，设置权限等（均由Erupt提供），可以用多人管理，也可以利用角色控制菜单的访问

# 使用

1. 下载jre或者jdk并配置环境变量，这部分不再提供详细教程
2. 拉取项目代码或者直接下载发行版提供的jar包
3. 填写 `application.yml` 配置文件 (非必须，如果需要请看下文，不需要上文的完整配置)
4. java -jar 启动 jar 即可
5. 访问 `http://localhost:8080` 账号密码为admin/admin

```yaml
server:
  port: 8080              # 项目端口
run:
  pageSize: 20         # 搜索结果分页大小
  upload-path: ${user.dir}/upload     # 公告图片的保存位置    ${user.dir} 是jar包所在的路径，请不要改这部分，需要配置的就是 /upload
  napcat-web: "http://127.0.0.1:6099"     # napcat的web地址，可以将napcat的页面嵌入到项目首页
```

如果你的napcat就是本地运行且端口为6099，则可以不配置napcat-web这一项

如果napcat符合条件，且对其余配置没有需求，可以不创建application.yml，直接启动 jar

# 写在最后

- napcat 说明文档 [napcat快速开始](https://napcat.napneko.icu/guide/start-install)
- Erup 官方文档 [Erupt文档](https://www.yuque.com/erupts/erupt)
- Shiro 框架官网 [Shiro快速开始](https://misakatat.github.io/shiro-docs/)