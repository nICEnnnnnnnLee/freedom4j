<h1 align="center">  
    <strong>
        Freedom
    </strong>
</h1>
<p align="center">
    端到端数据流量伪装加密研究
  <br/>
    <strong>仅供学习研究使用，请勿用于非法用途</strong>
</p>


## :star:相关Repo
| 项目名称  | 简介 | 
| ------------- | ------------- |   
| [freedom4py](https://github.com/nICEnnnnnnnLee/freedom4py)  |  python3实现，包含local端、remote端  | 
| [freedom4j](https://github.com/nICEnnnnnnnLee/freedom4j)  |  java实现，包含local端、remote端  | 
| [freedom4NG](https://github.com/nICEnnnnnnnLee/freedom4NG)  | Android java实现，仅包含local端；单独使用可作为DNS、Host修改器 | 
 




## :star:一句话说明  
将本地SOCKS5代理数据伪装成指向远程端的HTTP(S) WebSocket流量。

## :star:简介  
+ 在配置正确的情况下，python3、java、Android版本的local端和remote端可以配合使用。  
+ local端支持国内/外IP识别，国内IP可以选择直连。  
+ local端到remote端可以套上一层HTTP(S)，表现行为与Websocket无异，经测试**可过CDN与Nginx**。  
+ local端到remote端支持简单的用户名密码验证。  

## :star:缺陷  
+ 仅支持TCP，不支持UDP
+ Socks5代理仅支持IPv4，不支持IPv6
+ 不支持同一JVM同时运行local端和remote端


## :star:如何下载  
[Release](https://github.com/nICEnnnnnnnLee/freedom4j/releases)  

## :star:如何配置  


<details>
<summary>local端配置`app.config`</summary>



```
# 指示该配置用于本地/远程
configType = local
# 本地监听端口
localPort = 80
# 远程IP
remoteHost = 127.0.0.1
# 远程端口
remotePort = 443
# 用户名密码盐值(需要与服务器一致)
salt = 4567
username = username
password = pwd

# 国内ip是否直连
dircectIfCN = false
# 与远程的连接是否采用SSL
useSSLRemote = true
# 是否验证SSL证书
verifySSLRemote = false

# 用于用于伪装HTTP头部， 当SSL启用时，domain最好与证书SNI一致
path = /
http_version = 1.1
domain = 127.0.0.1
port = 443
userAgent = Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/86.0
```
</details>

<details>
<summary>remote端配置`app.server.config`</summary>



```
# 指示该配置用于本地/远程
configType = remote

# 监听端口
port = 443
# 用户名密码盐值(需要与服务器一致)
salt = 4567
# 是否使用SSL
useSSL = false

# SSL证书的相关配置，当SSL启用时有效
sslCertPath = D:\Workspace\cert\yourdomain.pem
sslKeyPath = D:\Workspace\cert\yourdomain.pkcs8

# 自签名的域名证书，当SSL启用时且没有配置证书路径时有效
# a fully qualified domain name
#fqdn = www.test.com


# 合法用户
# 格式为 users.<用户名> = <密码>
#users.admin = admin
#users.username = pwd
users.username = pwd

```
</details>




## :star:如何运行  
+ 运行本地/远程端  
命令行指定配置的路径，配置将决定程序以本地还是远程的身份运行。
```
## java -jar Freedom.jar <配置路径>
java -jar Freedom.jar app.config
```
