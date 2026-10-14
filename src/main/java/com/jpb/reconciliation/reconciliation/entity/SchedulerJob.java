package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime; 

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "rcn_schedule_mast")
@Data
@ToString
public class SchedulerJob {

    @Id
    @Column(name = "RSM_SCHEDULE_ID")
    private Long scheduleId;

    @Column(name = "RSM_NAME")
    private String name;

    @Column(name = "RSM_SCHEDULE_TYPE")
    private String scheduleType;

    @Column(name = "RSM_FILE_ID")
    private Long fileId;

    @Column(name = "RSM_DAYS")
    private String days;

    @Column(name = "RSM_DEP_JOB_ID")
    private Long depJobId;

    @Column(name = "RSM_DEP_FLG")
    private String depFlag;

    @Column(name = "RSM_RETRY_FLG")
    private String retryFlag;

    @Column(name = "RSM_RETRY_CNT")
    private Integer retryCount;

    @Column(name = "RSM_RETRY_INTERVAL")
    private LocalDateTime retryInterval;

    @Column(name = "RSM_STATUS")
    private String status;

    @Column(name = "RSM_SCHEDULER_TIME")
    private LocalDateTime schedulerTime;

    @Column(name = "RSM_CURR_STAT")
    private String currentStatus;

    @Column(name = "RSM_INST_CODE")
    private Integer bnkCode;

    @Column(name = "RSM_INS_USER")
    private Integer insUser;

    @Column(name = "RSM_INS_DATE")
    private LocalDateTime insDate;

    @Column(name = "RSM_LUPD_USER")
    private Integer lupdUser;

    @Column(name = "RSM_LUPD_DATE")
    private LocalDateTime lupdDate;

    @Column(name = "RSM_INS_NAME")
    private String insName;


}
