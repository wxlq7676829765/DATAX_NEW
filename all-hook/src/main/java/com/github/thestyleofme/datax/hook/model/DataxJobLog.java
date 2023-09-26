package com.github.thestyleofme.datax.hook.model;

import javax.persistence.*;

import lombok.*;

/**
 * <p>
 * datax数据同步统计类
 * </p>
 *
 * @author isacc 2020/5/13 17:28
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "dk_datax_job_log")
public class DataxJobLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * datax的jobid
     */
    private Long jobId;
    /**
     * job名称
     */
    private String jobName;
    /**
     * 执行此job的datax节点的ip
     */
    private String ip;
    /**
     * 执行此job的datax节点
     */
    private String node;
    /**
     * 执行此job的log日志文件所在路径
     */
    private String logPath;
    /**
     * log具体内容
     */
    private String logContent;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getLogContent() {
        return logContent;
    }

    public void setLogContent(String logContent) {
        this.logContent = logContent;
    }

    /*// 内部 builder 类
    public static class Builder {
        private String id;
        private String ip;

        private String dateformat;

        public Builder(ColumnType columnType) {
            this.columnType = columnType;
        }

        public Builder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public Builder columnValue(String columnValue) {
            this.columnValue = columnValue;
            return this;
        }

        public Builder dateformat(String dateformat) {
            this.dateformat = dateformat;
            return this;
        }

        public DataxJobLog build() {
            return new DataxJobLog(this);
        }
    }*/

    public DataxJobLog jobId(Long jobid){
        this.jobId = jobid;
        return this;
    }

    public DataxJobLog ip(String ip){
        this.ip = ip;
        return this;
    }

    public DataxJobLog build(){
        return this;
    }

    public  static DataxJobLog builder(){
        return new DataxJobLog();
    }
}
