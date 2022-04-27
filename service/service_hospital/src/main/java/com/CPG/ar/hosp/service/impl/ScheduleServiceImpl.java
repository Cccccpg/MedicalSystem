package com.CPG.ar.hosp.service.impl;

import com.CPG.ar.entity.hosp.Schedule;
import com.CPG.ar.hosp.repository.ScheduleRepository;
import com.CPG.ar.hosp.service.ScheduleService;
import com.CPG.ar.vo.hosp.ScheduleQueryVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    //上传排班接口
    @Override
    public void save(Map<String, Object> paramMap) {
        //将paramMap转换成Schedule对象
        String paramMapString = JSONObject.toJSONString(paramMap);
        Schedule schedule = JSONObject.parseObject(paramMapString, Schedule.class);

        //根据医院编号和排班编号查询
        Schedule scheduleExist = scheduleRepository.getScheduleByHoscodeAndHosScheduleId(schedule.getHoscode(),schedule.getHosScheduleId());
        //判断
        if (scheduleExist != null){
            scheduleExist.setUpdateTime(new Date());
            scheduleExist.setIsDeleted(0);
            scheduleExist.setStatus(1);
            scheduleRepository.save(scheduleExist);
        }else {
            schedule.setCreateTime(new Date());
            schedule.setUpdateTime(new Date());
            schedule.setIsDeleted(0);
            scheduleRepository.save(schedule);
        }
    }

    //条件查询带分页
    @Override
    public Page<Schedule> findPageSchedule(int page, int limit, ScheduleQueryVo scheduleQueryVo) {
        //创建Pageable对象，设置当前页和每页记录数
        //0是第一页
        Pageable pageable = PageRequest.of(page-1,limit);
        Schedule schedule = new Schedule();
        //将scheduleQueryVo中的值复制到schedule对象中
        BeanUtils.copyProperties(scheduleQueryVo,schedule);
        schedule.setIsDeleted(0);
        schedule.setStatus(1);
        //创建Example对象
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)//模糊查询
                .withIgnoreCase(true);//忽略大小写
        Example<Schedule> example = Example.of(schedule,matcher);
        Page<Schedule> all = scheduleRepository.findAll(example, pageable);
        return all;
    }

    //删除排班
    @Override
    public void remove(String hoscode, String hosScheduleId) {
        //根据医院编号和排班编号查询
        Schedule scheduleByHoscodeAndHosScheduleId = scheduleRepository.getScheduleByHoscodeAndHosScheduleId(hoscode, hosScheduleId);
        if (scheduleByHoscodeAndHosScheduleId != null){
            scheduleRepository.deleteById(scheduleByHoscodeAndHosScheduleId.getId());
        }
    }
}
