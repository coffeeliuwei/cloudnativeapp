DROP TABLE IF EXISTS  `express`;
CREATE TABLE `express` (
  `express_id` varchar(32) NOT NULL COMMENT '快递标识',
  `order_id` varchar(32) NOT NULL COMMENT '订单编号',
  `express_weight` float NOT NULL COMMENT '重量',
  `express_status` char(1) NOT NULL COMMENT '状态',
  `express_amount` float NOT NULL COMMENT '快递费用',
  `express_address` varchar(200) DEFAULT NULL COMMENT '收货地址',
  PRIMARY KEY (`express_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='快递';

DROP TABLE IF EXISTS  `track`;
CREATE TABLE `track` (
  `track_id` varchar(32) NOT NULL COMMENT '轨迹编号',
  `track_status` char(1) NOT NULL COMMENT '轨迹状态',
  `track_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '轨迹时间',
  `track_show` varchar(32) NOT NULL COMMENT '轨迹说明',
  `express_id` varchar(32) NOT NULL COMMENT '快递标识',
  PRIMARY KEY (`track_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



insert into `express`(`express_id`,`order_id`,`express_weight`,`express_status`,`express_amount`,`express_address`) values
('33224455','44556677',2,'2',25,'中国北京朝阳阳呼家楼'),
('45456561','76561256',1,'1',12,'中国北京朝阳阳呼家楼');

insert into `track`(`track_id`,`track_status`,`track_time`,`track_show`,`express_id`) values
('11223344','1','2021-10-20 16:01:53','已发出','33224455'),
('22334455','2','2021-10-21 16:02:38','到达','到达中转站石家庄');