# 合同统计提醒系统

一个基于 Java + Spring Boot 的本地 Web 应用，用于录入项目合同、自动计算收付款统计，并在每个缴费周期前生成提醒。

## 功能概览

- 录入合同基础信息、费用信息和日期信息
- 自动计算每个缴费周期应收/应付款
- 自动计算合同期总金额和计费周期数
- 在每个缴费周期前 15 天生成提醒
- 支持将未完成事项加入欠费清单
- 数据默认持久化到本地 `data/contracts.json`

## 本地运行

```bash
mvn spring-boot:run
```

启动后访问 [http://localhost:62781](http://localhost:62781)。

## 打包

```bash
mvn clean package
```

打包成功后生成 `target/contract-reminder-1.0.0.jar`。

## Windows 部署

1. 安装 Java 17 或更高版本
2. 将 jar 文件复制到目标机器
3. 双击或执行命令启动：

```bash
java -jar contract-reminder-1.0.0.jar
```

应用启动后在浏览器打开 `http://localhost:62781` 即可。
