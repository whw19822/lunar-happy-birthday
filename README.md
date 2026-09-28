# 岁岁 · 农历生日

原生 Android 应用，支持 Android 8.0 及以上。米白与松绿色界面，无账号、无网络权限，生日保存在手机本地。

## 功能

- 添加、编辑、删除农历生日，支持闰月。
- 显示下一次公历生日与倒计时，按生日先后排序。
- 每条生日可设置当天或提前 1 / 3 / 7 / 14 天预告，生日当天仍会提醒，支持时间与独立开关。
- 三级提醒：轻提醒（安静通知）、重要提醒（默认，声音和振动）、强提醒（当天持续闹钟响铃，最长 10 分钟）。
- 重要和强提醒支持不重复、每隔 1 小时或 3 小时，当天最多追加两次，不跨午夜。
- 通知和首页提供“10 分钟后”“已送祝福”；响铃期间提供“停止响铃”。确认祝福后取消本次生日剩余提醒，下一年度自动恢复。
- 自动安排下一年度提醒，开机、时区变化、时间调整、应用更新后重新安排。
- 通知权限设置和测试通知。

## 日期规则

闰月生日在没有对应闰月的年份按同名普通月庆祝；三十遇小月按廿九庆祝。农历生日每年重新换算，提醒时间按手机当前时区计算。如果提前预告时刻已过，仍安排生日当天提醒；当天及追加提醒全部已过后安排下一年。已排定但因后台限制延迟的提醒仅在原定当天补发。稍后提醒状态会在打开应用和重启后保留。

使用 MIT 开源库 [6tail/lunar-java 1.7.7](https://github.com/6tail/lunar-java) 换算农历。回归测试覆盖 2026 / 2027 春节、2026 中秋、2025 闰六月、2024–2030 各月三十和闰月回退，以及提前提醒跨年。避免依赖不同安卓版本内置 ICU 的农历差异。

## 构建

安装 JDK 17、Android SDK 36，然后执行：

```sh
ANDROID_HOME=/path/to/android/sdk ./gradlew :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest
```

安装包：`app/build/outputs/apk/debug/app-debug.apk`。

连接测试设备或模拟器后执行日期回归检查：

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.whw.lunarbirthday.test/com.whw.lunarbirthday.DateChecks
```

测试成功会输出 `PASS`，失败会输出 `FAIL`。

## 提醒注意事项

Android 13 及以上需允许通知。Android 12 及以上可在“提醒设置 → 闹钟权限”允许精确闹钟。重要/强提醒获准后使用精确闹钟；未授权时使用非精确通知，强提醒不会自动持续响铃，界面明确提示状态。

强提醒通过 `mediaPlayback` 前台服务播放系统闹钟铃声并振动，提供持续可见的停止入口；采用系统闹钟音量，不更改音量或绕过免打扰，最长 10 分钟自动停止。多个生日同时响铃时共享音频播放，逐条停止或确认不会误停其他生日。服务被系统限制启动时保留可操作通知。

省电策略、通知渠道静音、免打扰和厂商后台限制仍可能影响提醒。系统强行停止后需重新打开应用。尚未验证各厂商真机的长期后台送达。

平台依据：[安卓闹钟权限与调度](https://developer.android.com/develop/background-work/services/alarms)、[前台服务后台启动限制](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)。

当前提供可直接安装的调试签名 APK；正式上架需配置发布签名。数据不上传，卸载会清除生日记录。

## 1.1 验证

日期与提醒状态回归共 361 项通过，包含预告后当天提醒、次数上限、不跨午夜、延后持久化、旧通知操作拒绝、确认后安排下一年、关闭提醒和重新开启后的完成状态保留。
