package com.CPG.ar.task.scheduled;

import com.CPG.common.rabbit.constant.MqConst;
import com.CPG.common.rabbit.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling   //开启定时任务操作
public class ScheduledTask {

    @Autowired
    private RabbitService rabbitService;


    //每天八点执行该方法，就医提醒
    //cron表达式，设置执行间隔
    //每天8点的表达式为：0 0 8 * * ?
    //这里为了测试方便，就每隔30秒发送一次
    @Scheduled(cron = "0/30 * * * * ?")
    public void taskPatient(){
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_8, "");
    }
}
