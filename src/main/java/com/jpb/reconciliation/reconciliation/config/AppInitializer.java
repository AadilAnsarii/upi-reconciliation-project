package com.jpb.reconciliation.reconciliation.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jpb.reconciliation.reconciliation.service.SchedulerService;

@Component
public class AppInitializer {

    @Autowired
    SchedulerService schedulerService;

    @PostConstruct
    public void init(){
        schedulerService.scheduleTasks();
    }
}
