-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI知识库文档', '3', '1', 'doc', 'system/doc/index', 1, 0, 'C', '0', '0', 'system:doc:list', '#', 'admin', sysdate(), '', null, 'AI知识库文档菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI知识库文档查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'system:doc:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI知识库文档新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'system:doc:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI知识库文档修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'system:doc:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI知识库文档删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'system:doc:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI知识库文档导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'system:doc:export',       '#', 'admin', sysdate(), '', null, '');