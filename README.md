# SearchEngine
  使用java语言实现的一个搜索引擎的小项目，用于搜索《游民星空》中所有的游戏资讯和新闻信息，pom.xml文件中是需要下载的依赖
  

## 直接拉取下载项目到本地
```
git clone git@github.com:Junqiao77/SearchEngine.git
```
## 第一步 实现网页爬取
  我使用的是用java爬取了《游民星空》中所有的游戏资讯和新闻信息，你应该根据你想爬取的网页的内容修改这一部分的内容
代码在src/main/java/webclawer

## 第二步 构建索引
  根据爬取的相关的字段，构建对应的索引，用于后面的检索策略
  代码在src/main/java/IndexBuilder

## 第三步 实现简单的搜索查询
  根据索引进行简单的文本检索
  代码在src/main/java/SearchEngine
