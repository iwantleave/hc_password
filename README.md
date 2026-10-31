# HC 密码保险箱

一个使用 Java Swing 开发的本地密码管理软件，密码数据保存在 SQLite 数据库中，敏感字段使用 AES-256-GCM 加密。

## 功能

- 首次启动时创建主密码，之后使用主密码解锁
- 密码记录的添加、编辑、删除和查询
- 按标题、用户名、网址、分类进行关键字搜索
- 按分类筛选记录，支持自定义分类
- 随机密码生成器
  - 长度 8-64 位
  - 可选小写字母、大写字母、数字、特殊符号
  - 保证每种已选字符类型至少出现一次
- 一键复制密码或用户名
  - 复制密码后 30 秒自动清空剪贴板
  - 仅在剪贴板内容仍是复制的密码时才清空
- 打开记录中保存的网址
- 修改主密码，并使用新密钥重新加密全部记录
- 手动锁定保险箱
- 双击表格行编辑记录
- 鼠标右键菜单
- Windows 风格的桌面界面，使用 Nimbus 外观和微软雅黑字体

## 技术栈

| 技术 | 版本/说明 |
| --- | --- |
| Java | 17 |
| UI | Java Swing |
| 数据库 | SQLite |
| SQLite 驱动 | `org.xerial:sqlite-jdbc:3.45.3.0` |
| 密钥派生 | PBKDF2WithHmacSHA256 |
| 数据加密 | AES-256-GCM |
| 测试 | JUnit 5 |
| 构建 | Maven |

## 快速开始

### 环境要求

- JDK 17 或更高版本
- Maven 3.8 或更高版本

### 方式一：在 IntelliJ IDEA 中运行

1. 使用 IntelliJ IDEA 打开项目根目录。
2. 等待 Maven 自动导入依赖。
3. 运行 `src/main/java/org/example/Main.java` 中的 `main` 方法。

### 方式二：命令行运行

```bash
mvn clean package
java -jar target/hc-password.jar
```

也可以直接通过 Maven 启动：

```bash
mvn compile exec:java
```

首次启动时会要求创建主密码。主密码至少 8 个字符。

## 使用说明

1. 创建主密码并妥善保存。主密码一旦遗忘，已保存的数据无法恢复。
2. 点击“新增”添加密码记录。
3. 在记录表单中填写标题、用户名、密码、网址、分类和备注。
4. 点击密码输入框旁的“生成”按钮，可以生成随机密码。
5. 在主界面搜索框或分类下拉框中筛选记录。
6. 选中记录后，可以复制密码、编辑或删除。
7. 双击记录可以直接编辑。
8. 点击“锁定”或按 `Ctrl+L` 锁定保险箱，重新解锁后才能继续使用。
9. 通过“文件 > 修改主密码”更换主密码，所有记录会自动重新加密。

### 快捷键

| 快捷键 | 功能 |
| --- | --- |
| `Ctrl+N` | 新增记录 |
| `Ctrl+E` | 编辑选中记录 |
| `Ctrl+F` | 聚焦搜索框 |
| `Ctrl+C` | 在记录表格中复制密码 |
| `Ctrl+L` | 锁定保险箱 |
| `Delete` | 删除选中记录 |
| `Enter` | 编辑选中记录 |
| `Esc` | 关闭当前对话框 |

## 数据存储位置

默认数据库文件：

```text
Windows: %USERPROFILE%\.hc_password\vault.db
例如: C:\Users\<用户名>\.hc_password\vault.db
```

SQLite 启用 WAL 模式，运行时可能同时存在以下文件：

```text
vault.db
vault.db-wal
vault.db-shm
```

如果需要备份数据，建议先退出程序，再备份整个 `.hc_password` 目录。

### 自定义数据目录

支持通过启动参数修改数据库位置：

```bash
# 便携模式：数据库保存到 jar 同级 data 目录
java -jar target/hc-password.jar --portable

# 自定义目录：数据库保存到指定目录下的 vault.db
java -jar target/hc-password.jar --data-dir "D:\HC密码数据"

# 查看帮助
java -jar target/hc-password.jar --help
```

便携模式下的数据库路径：

```text
<jar 所在目录>\data\vault.db
```

使用说明：

- `--portable` 与 `--data-dir` 不能同时使用。
- 启动时会检查数据目录是否可创建和写入；如果 jar 位于 `Program Files` 等只读目录，请改用默认目录或 `--data-dir`。
- 便携模式启动时会显示数据库路径并提示备份，确认后才会继续启动。
- 切换数据目录后不会自动迁移旧数据；如需继续使用原保险箱，请先退出程序，再手动复制整个数据目录。
- 备份时请退出程序，并复制整个数据目录，而不是只复制 `vault.db`，以免遗漏 `vault.db-wal` 和 `vault.db-shm`。
- 使用 Maven 启动时可通过 `-Dexec.args` 传递参数，例如 `mvn compile exec:java -Dexec.args="--portable"`。

## 数据库结构

### meta 表

保存保险箱元数据。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `key` | TEXT | 元数据键 |
| `value` | TEXT | 元数据值 |

主要元数据：

- `master_salt`：Base64 编码的随机盐
- `kdf_iterations`：PBKDF2 迭代次数
- `master_verifier`：用于验证主密码的加密验证串
- `schema_version`：数据库结构版本

### entries 表

保存密码记录。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | INTEGER | 主键，自增 |
| `title` | TEXT | 标题，明文 |
| `username` | TEXT | 用户名，明文 |
| `password` | TEXT | 密码，AES-GCM 加密后以 Base64 保存 |
| `url` | TEXT | 网址，明文 |
| `category` | TEXT | 分类，明文 |
| `notes` | TEXT | 备注，AES-GCM 加密后以 Base64 保存 |
| `created_at` | TEXT | 创建时间 |
| `updated_at` | TEXT | 更新时间 |

## 安全设计

### 主密码与密钥

- 主密码不保存在任何位置。
- 使用 `PBKDF2WithHmacSHA256` 派生密钥。
- 每次创建保险箱或修改主密码都会生成新的 16 字节随机盐。
- 迭代次数默认为 210,000 次。
- 派生结果为 256 位 AES 密钥，仅在程序运行期间保存在内存中。
- 锁定保险箱时会丢弃内存中的密钥。

### 数据加密

- 使用 `AES/GCM/NoPadding` 加密。
- 每条记录每次加密都会生成新的 12 字节随机 IV。
- GCM 认证标签长度为 128 位。
- 存储格式为 `Base64(IV + 密文 + 认证标签)`。
- 加密字段被篡改时，GCM 校验会失败，程序会报告解密错误。

### 主密码验证

数据库不保存主密码或主密码哈希。解锁流程如下：

1. 从数据库读取盐和迭代次数。
2. 使用用户输入的主密码派生候选密钥。
3. 使用候选密钥解密数据库中保存的验证串。
4. 验证串匹配则解锁成功，否则提示主密码错误。

### 修改主密码

修改主密码时：

1. 使用旧密钥解密全部密码和备注。
2. 生成新盐，并从新主密码派生新密钥。
3. 使用新密钥重新加密全部敏感字段。
4. 在同一个数据库事务中更新所有记录和元数据。

如果事务失败，数据库会回滚。

### 剪贴板保护

复制密码后启动 30 秒定时器。定时器触发时会检查剪贴板内容，只有内容仍是被复制的密码时才清空，避免误删用户后来复制的内容。

## 安全限制

本项目定位为本地个人密码管理工具，未经过专业安全审计，请注意以下限制：

- SQLite 数据库文件本身没有整体加密。
- `title`、`username`、`url`、`category` 和时间字段使用明文存储。
- `password` 和 `notes` 已加密，但其他字段可能泄露账号用途或网站信息。
- 解锁后，解密后的记录会存在于 Java 内存中。
- 剪贴板中的内容可能被其他程序读取，因此提供 30 秒自动清除。
- 主密码遗忘后无法恢复任何数据，没有找回或重置机制。
- 请勿将数据库文件提交到 Git 仓库或上传到公共位置。

## 项目结构

```text
hc_password/
├── pom.xml
├── README.md
└── src/
    ├── main/java/org/example/
    │   ├── Main.java
    │   ├── AppController.java
    │   ├── config/
    │   │   ├── DataDirectory.java
    │   │   └── LaunchOptions.java
    │   ├── db/
    │   │   ├── DatabaseManager.java
    │   │   └── EntryRepository.java
    │   ├── model/
    │   │   └── PasswordEntry.java
    │   ├── security/
    │   │   ├── CryptoService.java
    │   │   ├── PasswordGenerator.java
    │   │   └── VaultException.java
    │   ├── service/
    │   │   └── VaultService.java
    │   └── ui/
    │       ├── ChangeMasterPasswordDialog.java
    │       ├── EntryDialog.java
    │       ├── EntryTableModel.java
    │       ├── LoginDialog.java
    │       ├── MainFrame.java
    │       ├── PasswordGeneratorDialog.java
    │       └── UiTheme.java
    └── test/java/org/example/
        ├── config/
        │   ├── DataDirectoryTest.java
        │   └── LaunchOptionsTest.java
        ├── security/
        │   ├── CryptoServiceTest.java
        │   └── PasswordGeneratorTest.java
        └── service/
            └── VaultServiceTest.java
```

## 测试

运行全部测试：

```bash
mvn test
```

当前测试覆盖：

- AES-GCM 加密与解密
- 不同 IV 产生不同密文
- 错误密钥无法解密
- 不同盐派生不同密钥
- 密码生成器长度和字符类型
- 数据库记录的完整增删改查
- 数据持久化
- 错误主密码校验
- 修改主密码后的重新加密
- 未解锁时拒绝访问数据
- 启动参数解析和数据目录选择
- 数据目录创建与路径校验

## 可扩展方向

- 数据库文件整体加密或接入系统密钥链
- 主密码错误次数限制和延迟
- 密码强度评估
- 密码泄露检测
- 导入/导出加密备份
- 自动锁定
- 回收站与误删恢复
- 多语言支持
- 打包为 Windows 安装程序

## 许可证

本项目采用 GNU General Public License v3.0 许可证，详见 [LICENSE](LICENSE)。

