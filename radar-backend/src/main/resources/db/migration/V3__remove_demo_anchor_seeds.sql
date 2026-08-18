-- 清理 V1 历史演示主播种子。
--
-- 游客首页演示主播由 radar.live.guest-demo 配置和公开快照服务提供，
-- 不再依赖 live_anchor 中的固定演示主播记录。
-- 只删除没有用户关注关系的历史种子，避免留下孤立关注关系或误删用户数据。

DELETE FROM live_anchor
WHERE (
       (platform = 'BILIBILI' AND room_id = '22637261')
    OR (platform = 'DOUYU' AND room_id = '9999')
    OR (platform = 'HUYA' AND room_id = '998')
    OR (platform = 'DOUYIN' AND room_id = '369324308707')
)
AND NOT EXISTS (
    SELECT 1
    FROM user_follow_anchor
    WHERE user_follow_anchor.anchor_id = live_anchor.id
);

