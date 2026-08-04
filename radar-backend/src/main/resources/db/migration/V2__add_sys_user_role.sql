-- 为系统用户增加账号角色。

ALTER TABLE sys_user
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER'
        COMMENT '账号角色：USER普通用户，ADMIN管理员'
        AFTER status;

CREATE INDEX idx_sys_user_role
    ON sys_user (role);
