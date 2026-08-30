# IDEA 重新导入指南

旧 `.idea` 包含过期 Module、旧 Main class 和非 JDK 17 配置，不再维护。请按以下步骤由用户本人重建 IDEA 元数据：

1. 关闭 IntelliJ IDEA。
2. 将现有 `.idea` 备份到项目目录之外。
3. 删除项目根目录的旧 `.idea`。
4. 从唯一 Maven Root `MYTrafficDataHub_20260829/pom.xml` 重新打开工程。
5. 在 Maven 工具窗口执行 Reload All Maven Projects。
6. 设置 Project SDK = JDK 17，Language level = 17。
7. 设置 Maven Runner JDK = JDK 17。
8. 确认 `Application` Module SDK = Project SDK 17。
9. 新建 Spring Boot Run Configuration：
   - Module：`Application`
   - Main class：`com.mytransitgps.MyTrafficDataHubApplication`
   - Working directory：项目根目录
   - Active profile：`longrun`（仅在人工真实运行时）

重导入后应只看到 `Application`、`Common`、`BusGPS`、`CIQ` 四个 Maven Module。
