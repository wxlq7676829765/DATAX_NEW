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
@Table(name = "dk_datax_statistics")
public class DataxStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 执行id，其他调度平台的执行id
     */
    private Long execId;
    /**
     * datax的jobid
     */
    private Long jobId;
    /**
     * datax执行的json文件名
     */
    private String jsonFileName;
    /**
     * job名称
     */
    private String jobName;
    /**
     * datax执行的json路径
     */
    private String jobPath;
    /**
     * datax的json内容
     */
    private String jobContent;
    /**
     * reader插件名称
     */
    private String readerPlugin;
    /**
     * writer插件名称
     */
    private String writerPlugin;
    /**
     * 任务启动时刻
     */
    private String startTime;
    /**
     * 任务结束时刻
     */
    private String endTime;
    /**
     * 任务总计耗时，单位s
     */
    private String totalCosts;
    /**
     * 任务平均流量
     */
    private String byteSpeedPerSecond;
    /**
     * 记录写入速度
     */
    private String recordSpeedPerSecond;
    /**
     * 读出记录总数
     */
    private Long totalReadRecords;
    /**
     * 读写失败总数
     */
    private Long totalErrorRecords;
    /**
     * 脏数据即未同步成功的数据
     */
    private String dirtyRecords;

    private Long projectId;
    /**
     * 项目空间ID
     */
    private Long taskInstanceId;
    /**
     * 节点任务实例ID
     */
    private Long processId;
    /**
     * 任务名称ID
     */
    private Long processInstanceId;
    /**
     * 工作流实例ID
     */


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExecId() {
        return execId;
    }

    public void setExecId(Long execId) {
        this.execId = execId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJsonFileName() {
        return jsonFileName;
    }

    public void setJsonFileName(String jsonFileName) {
        this.jsonFileName = jsonFileName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobPath() {
        return jobPath;
    }

    public void setJobPath(String jobPath) {
        this.jobPath = jobPath;
    }

    public String getJobContent() {
        return jobContent;
    }

    public void setJobContent(String jobContent) {
        this.jobContent = jobContent;
    }

    public String getReaderPlugin() {
        return readerPlugin;
    }

    public void setReaderPlugin(String readerPlugin) {
        this.readerPlugin = readerPlugin;
    }

    public String getWriterPlugin() {
        return writerPlugin;
    }

    public void setWriterPlugin(String writerPlugin) {
        this.writerPlugin = writerPlugin;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getTotalCosts() {
        return totalCosts;
    }

    public void setTotalCosts(String totalCosts) {
        this.totalCosts = totalCosts;
    }

    public String getByteSpeedPerSecond() {
        return byteSpeedPerSecond;
    }

    public void setByteSpeedPerSecond(String byteSpeedPerSecond) {
        this.byteSpeedPerSecond = byteSpeedPerSecond;
    }

    public String getRecordSpeedPerSecond() {
        return recordSpeedPerSecond;
    }

    public void setRecordSpeedPerSecond(String recordSpeedPerSecond) {
        this.recordSpeedPerSecond = recordSpeedPerSecond;
    }

    public Long getTotalReadRecords() {
        return totalReadRecords;
    }

    public void setTotalReadRecords(Long totalReadRecords) {
        this.totalReadRecords = totalReadRecords;
    }

    public Long getTotalErrorRecords() {
        return totalErrorRecords;
    }

    public void setTotalErrorRecords(Long totalErrorRecords) {
        this.totalErrorRecords = totalErrorRecords;
    }

    public String getDirtyRecords() {
        return dirtyRecords;
    }

    public void setDirtyRecords(String dirtyRecords) {
        this.dirtyRecords = dirtyRecords;
    }


    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getTaskInstanceId() {
        return taskInstanceId;
    }

    public void setTaskInstanceId(Long taskInstanceId) {
        this.taskInstanceId = taskInstanceId;
    }

    public Long getProcessId() {
        return processId;
    }

    public void setProcessId(Long processId) {
        this.processId = processId;
    }

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
}
