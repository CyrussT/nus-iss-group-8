package com.group8.rbs.config;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.group8.rbs.service.scheduler.CreditRestore;

@Configuration
public class QuartzSchedulerConfig {

    @Bean
    public JobDetail jobDetail() {
        return JobBuilder.newJob(CreditRestore.class)
                .withIdentity("creditRestoreJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger trigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity("creditRestoreTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 ? * MON"))
                .build();
    }

}
