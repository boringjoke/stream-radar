-- StreamRadar 核心业务表及演示主播初始化。

CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(32) NOT NULL COMMENT '用户名，统一按小写保存',
    password_hash VARCHAR(100) NOT NULL COMMENT '密码摘要',
    email VARCHAR(255) NULL COMMENT '邮箱地址',
    nickname VARCHAR(64) NOT NULL COMMENT '用户昵称',
    avatar_path VARCHAR(512) NULL COMMENT '项目内头像静态资源相对路径或资源标识，不存外部 URL',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1启用，0停用',
    last_login_time DATETIME(3) NULL COMMENT '最后登录时间',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_email (email),
    KEY idx_sys_user_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '系统用户表';

CREATE TABLE live_anchor (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    platform VARCHAR(32) NOT NULL COMMENT '平台标识：BILIBILI、DOUYU、HUYA、DOUYIN',
    room_id VARCHAR(128) NOT NULL COMMENT '平台直播间标识',
    platform_uid VARCHAR(128) NULL COMMENT '平台主播用户标识',
    room_url VARCHAR(512) NOT NULL COMMENT '规范化直播间地址',
    anchor_name VARCHAR(128) NULL COMMENT '主播名称',
    avatar_url VARCHAR(512) NULL COMMENT '主播头像地址',
    cover_url VARCHAR(512) NULL COMMENT '直播封面地址',
    live_title VARCHAR(512) NULL COMMENT '当前或最后一次有效直播标题',
    online_count BIGINT NULL COMMENT '当前观看人数',
    live_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN' COMMENT '直播状态：LIVE、OFFLINE、UNKNOWN、ERROR',
    failure_count INT NOT NULL DEFAULT 0 COMMENT '连续采集失败次数',
    error_message VARCHAR(512) NULL COMMENT '最近一次采集错误摘要',
    last_check_time DATETIME(3) NULL COMMENT '最后检测时间',
    last_success_time DATETIME(3) NULL COMMENT '最后成功获取数据时间',
    status_change_time DATETIME(3) NULL COMMENT '状态最近一次变化时间',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_live_anchor_platform_room (platform, room_id),
    KEY idx_live_anchor_platform_status_check (platform, live_status, last_check_time),
    KEY idx_live_anchor_platform_uid (platform, platform_uid)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '直播主播表';

CREATE TABLE user_follow_anchor (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户主键，由业务代码维护关联关系',
    anchor_id BIGINT NOT NULL COMMENT '主播主键，由业务代码维护关联关系',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '关注创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '关注更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_follow_anchor_user_4anchor (user_id, anchor_id),
    KEY idx_user_follow_anchor_user (user_id),
    KEY idx_user_follow_anchor_anchor (anchor_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户关注主播关系表';

-- 演示主播只初始化平台、房间标识和规范地址，名称及状态由后续数据源采集填充。
INSERT INTO live_anchor (platform, room_id, room_url)
VALUES
    ('BILIBILI', '22637261', 'https://live.bilibili.com/22637261'),
    ('DOUYU', '9999', 'https://www.douyu.com/9999'),
    ('HUYA', '998', 'https://www.huya.com/998'),
    ('DOUYIN', '369324308707', 'https://live.douyin.com/369324308707');
