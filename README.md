# CookieVibe 浏览器

一个基于 Android WebView 的轻量级浏览器,主打 **Cookie 可视化 / 导入导出** 和 **浏览器指纹伪装**,适合做网页调试、账号迁移、隐私浏览。

包名:`com.cookievibe.viewer`

---

## 主要功能

### 1. 浏览基础
- 地址栏支持「网址 / 关键词搜索」自动识别,空格或无 `.` 的输入会走搜索引擎
- 搜索引擎可切换:Google / Bing / DuckDuckGo / 百度 / Startpage(默认 DuckDuckGo)
- 前进 / 后退 / 刷新 / 主页
- 进度条显示加载进度
- 长按链接 / 分享链接 / 复制网址
- 页内查找(Find in page)
- HTML5 全屏视频播放
- 文件下载(走系统 DownloadManager,自动带 Cookie 与 UA)
- 支持作为系统浏览器处理 `http` / `https` 链接

### 2. Cookie 工具(核心卖点)
- 「查看 Cookie」:列出当前页面所有 Cookie 的 name / value,长按条目可单条复制
- 一键**全部复制**为 `name=value; name2=value2` 格式
- 导出为 **JSON 数组**(`[{"name":..., "value":...}, ...]`)
- 导出为 **Header 格式**(可直接粘到 curl / Postman 的 `Cookie:` 请求头)
- **导入 Cookie**:支持两种格式
  - `name=value; name2=value2`
  - JSON 数组,可带 `domain / path / secure / httpOnly`
- 是否接受第三方 Cookie 可开关

### 3. 指纹 / UA 伪装
- 内置 UA 预设:WebView 默认 / Chrome Windows / Chrome Mac / Chrome Android / Safari iOS / Firefox / **自定义**
- 一键切换**桌面模式**(临时换 Chrome Windows UA 并重载)
- JS 指纹伪装开关(注入脚本改写):
  - `navigator.webdriver → false`
  - `navigator.platform` 跟随所选 UA
  - `navigator.languages / language` 跟随 `Accept-Language` 设置
  - `hardwareConcurrency`、`deviceMemory`
  - 补齐 `window.chrome`、`permissions.query` 对 `notifications` 的返回
  - ⚠️ 属于「延迟注入」,能糊弄大部分站点,对硬核反爬无效
- `Accept-Language` 请求头可自定义
- DNT(Do-Not-Track)开关

### 4. 隐私与数据
- **隐身模式**一键开关:关闭 Cookie 写入、清空会话 Cookie / 表单数据 / 历史
- 一键**清除**所有浏览数据(Cookie、缓存、历史、表单)
- 历史记录最多保留 500 条,书签无上限
- 是否允许 Mixed Content(HTTPS 页中的 HTTP 资源)
- SSL 错误放行开关(默认关;开启后遇到证书错误会弹窗让你选择继续/取消)

### 5. 其它
- 书签:当前页一键加入,书签列表可打开/删除
- 历史记录:列表查看、点击重新打开、可清空
- 加载图片开关(省流量)
- 支持深色模式(基于 WebView `ALGORITHMIC_DARKENING`)
- 允许系统备份(`allowBackup=true`)

---

## 项目结构

```
app/src/main/java/com/cookievibe/viewer/
├── MainActivity.kt          # 主浏览页 + WebView 配置 + 菜单逻辑
├── CookieActivity.kt        # Cookie 列表 / 复制 / 导出
├── BookmarksActivity.kt     # 书签列表
├── HistoryActivity.kt       # 历史列表
├── ListBaseActivity.kt      # 书签/历史共用基类
├── SettingsActivity.kt      # 设置页
├── db/Store.kt              # 基于 SharedPreferences + JSON 的书签/历史存储
├── prefs/Prefs.kt           # 偏好项封装
└── web/
    ├── UserAgents.kt        # UA 预设
    └── FingerprintScript.kt # 指纹伪装 JS
```

存储方案:直接用 `SharedPreferences` 把书签/历史序列化成 JSON(`cvv_store`),没有引入 Room,体量极小。

---

## 权限

- `INTERNET` / `ACCESS_NETWORK_STATE`:联网
- `WRITE/READ_EXTERNAL_STORAGE`:旧版 Android 下载时用(已按 SDK 版本收窄)
- `POST_NOTIFICATIONS`:下载完成通知

---

## 构建

```bash
./gradlew assembleDebug
# 产物:app/build/outputs/apk/debug/app-debug.apk
```

Gradle 8.6.1 + Kotlin 1.9.25。

---

## 使用小贴士

- **迁移账号**:在网页登录后,菜单 → 查看 Cookie → 导出为 JSON,拿到另一台设备菜单 → 导入 Cookie,再刷新即可免密登录
- **curl / Postman 调试**:导出为 Header,直接粘到 `Cookie:` 请求头
- **防指纹**:设置里同时打开「伪装指纹 JS」并把 UA 改成对应平台,效果更自然
- **清理**:设置页底部有「立即清除数据」,跟菜单里的效果一致
