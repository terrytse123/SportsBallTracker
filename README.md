# 球類比賽自動追蹤拍攝工具

Android 專案（CameraX + ML Kit Object Detection & Tracking）

## 功能
- 即時鏡頭預覽
- 使用 ML Kit 即時偵測並追蹤物件（含追蹤 ID）
- 畫面疊加綠色追蹤框
- 支援錄影（含音訊）
- 前後鏡頭切換
- 橫向模式（適合拍攝比賽）

## 如何建置 APK
1. 安裝 [Android Studio](https://developer.android.com/studio)
2. 開啟此資料夾（SportsBallTracker）
3. 等待 Gradle 同步完成
4. 連接手機或開啟模擬器
5. 點擊 Run 或 Build > Build Bundle(s) / APK(s) > Build APK(s)

## 注意事項
- 需要相機與麥克風權限
- ML Kit 通用物件偵測對「球」的專用效果有限，建議之後替換成自訂 YOLO 模型（針對籃球、足球等訓練）以提升準確度
- 錄影檔案儲存在 App 內部儲存空間

## 後續可擴充
- 自訂球類偵測模型
- 數位變焦 / 裁切跟隨球
- 連接雲台控制
- 多球追蹤與統計
