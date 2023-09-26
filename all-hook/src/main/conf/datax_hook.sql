/*
 Navicat Premium Data Transfer

 Source Server         : local_mysql5.7.26
 Source Server Type    : MySQL
 Source Server Version : 50726
 Source Host           : localhost:3306
 Source Schema         : datax_admin

 Target Server Type    : MySQL
 Target Server Version : 50726
 File Encoding         : 65001

 Date: 28/12/2020 19:59:26
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for datax_job_log
-- ----------------------------
DROP TABLE IF EXISTS `dk_datax_job_log`;

CREATE TABLE `dk_datax_job_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `job_id` bigint(20) NOT NULL DEFAULT '-1' COMMENT 'datax jobid，最好保持唯一性',
  `job_name` varchar(50) COLLATE utf8mb4_bin NOT NULL COMMENT 'datax job名称',
  `ip` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT 'datax节点的ip地址',
  `node` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '执行此job的datax节点',
  `log_path` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '执行此job的log日志文件所在路径',
  `log_content` mediumtext COLLATE utf8mb4_bin COMMENT 'log具体内容',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `datax_log_u1` (`job_id`,`ip`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='数据同步任务日志表';
-- ----------------------------
-- Table structure for datax_statistics
-- ----------------------------
DROP TABLE IF EXISTS `dk_datax_statistics`;
CREATE TABLE `dk_datax_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exec_id` bigint(20) DEFAULT NULL COMMENT '执行id',
  `job_id` bigint(20) NOT NULL DEFAULT '-1' COMMENT '任务id',
  `json_file_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'datax执行的json文件名',
  `job_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'job名称',
  `reader_plugin` varchar(30) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'reader插件名称',
  `writer_plugin` varchar(30) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'writer插件名称',
  `start_time` varchar(63) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '任务启动时刻',
  `end_time` varchar(63) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '任务结束时刻',
  `total_costs` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '任务总计耗时，单位s',
  `byte_speed_per_second` varchar(80) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '任务平均流量',
  `record_speed_per_second` varchar(80) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '记录写入速度',
  `total_read_records` bigint(20) DEFAULT NULL COMMENT '读出记录总数',
  `total_error_records` bigint(20) DEFAULT NULL COMMENT '读写失败总数',
  `job_path` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'datax执行的json路径',
  `job_content` text COLLATE utf8mb4_bin COMMENT 'datax的json内容',
  `dirty_records` longtext COLLATE utf8mb4_bin COMMENT '脏数据即未同步成功的数据',
  `project_id` bigint(20) DEFAULT NULL COMMENT '项目空间ID',
  `task_instance_id` bigint(20) DEFAULT NULL COMMENT '节点任务实例ID',
  `process_id` bigint(20) DEFAULT NULL COMMENT '任务名称ID',
  `process_instance_id` bigint(20) DEFAULT NULL COMMENT '工作流实例ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=36032 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='数据同步统计信息表';

SET FOREIGN_KEY_CHECKS = 1;
