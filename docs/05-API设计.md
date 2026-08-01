# 后端API设计

## 登录

POST /api/auth/login

## 首页

GET /api/live/home

游客返回示例主播。

用户返回关注主播。

## 添加主播

POST /api/live/follow

参数：

url

## WebSocket

/ws/live

事件：

LIVE_STATUS_CHANGED
