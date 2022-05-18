package com.CPG.ar.hosp.service.impl;

import com.CPG.ar.common.exception.AppointmentRegisterException;
import com.CPG.ar.common.result.ResultCodeEnum;
import com.CPG.ar.entity.hosp.BookingRule;
import com.CPG.ar.entity.hosp.Department;
import com.CPG.ar.entity.hosp.Hospital;
import com.CPG.ar.entity.hosp.Schedule;
import com.CPG.ar.hosp.mapper.ScheduleMapper;
import com.CPG.ar.hosp.repository.ScheduleRepository;
import com.CPG.ar.hosp.service.DepartmentService;
import com.CPG.ar.hosp.service.HospitalService;
import com.CPG.ar.hosp.service.ScheduleService;
import com.CPG.ar.vo.hosp.BookingScheduleRuleVo;
import com.CPG.ar.vo.hosp.ScheduleOrderVo;
import com.CPG.ar.vo.hosp.ScheduleQueryVo;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper,Schedule> implements ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

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

    //查询排班规则数据
    @Override
    public Map<String, Object> getRuleSchedule(long page, long limit, String hoscode, String depcode) {
        //根据医院编号和科室编号做查询
        Criteria criteria = Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode);
        //根据工作日期workDate进行分组
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),//匹配字段
                Aggregation.group("workDate")//分组字段
                .first("workDate").as("workDate")
                //统计号源
                .count().as("docCount")
                .sum("reservedNumber").as("reservedNumber")
                .sum("availableNumber").as("availableNumber"),
                //排序
                Aggregation.sort(Sort.Direction.DESC,"workDate"),
                //实现分页
                Aggregation.skip((page-1)*limit),
                Aggregation.limit(limit)
        );
        //调用方法执行
        AggregationResults<BookingScheduleRuleVo> aggregateResult =
                mongoTemplate.aggregate(aggregation, Schedule.class, BookingScheduleRuleVo.class);
        List<BookingScheduleRuleVo> bookingScheduleRuleVoList = aggregateResult.getMappedResults();

        //分组查询的总记录数
        Aggregation totalAggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("workDate")
        );

        AggregationResults<BookingScheduleRuleVo> totalAggregationResult =
                mongoTemplate.aggregate(totalAggregation, Schedule.class, BookingScheduleRuleVo.class);
        int total = totalAggregationResult.getMappedResults().size();

        //把获取日期对应的星期
        for (BookingScheduleRuleVo bookingScheduleRuleVo : bookingScheduleRuleVoList){
            Date workDate = bookingScheduleRuleVo.getWorkDate();
            String dayOfWeek = this.getDayOfWeek(new DateTime(workDate));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);
        }

        //设置最终的数据并且返回
        Map<String, Object> result = new HashMap<>();
        result.put("bookingScheduleRuleList",bookingScheduleRuleVoList);
        result.put("total",total);

        //获取医院的名称
        String hosName = hospitalService.getHospName(hoscode);

        Map<String, String> baseMap = new HashMap<>();
        baseMap.put("hosname",hosName);
        result.put("baseMap",baseMap);
        return result;
    }

    //查询排班详细信息
    @Override
    public List<Schedule> getDetailSchedule(String hoscode, String depcode, String workDate) {
        //根据参数查询mongodb
        List<Schedule> scheduleList =
                scheduleRepository.findScheduleByHoscodeAndDepcodeAndWorkDate(hoscode,depcode,new DateTime(workDate).toDate());
        //把得到的list集合遍历，向里面设置其他的值：医院名称、科室名称、日期
        scheduleList.stream().forEach(item ->{
            this.packageSchedule(item);
        });
        return scheduleList;
    }

    //获取可预约的排班数据
    @Override
    public Map<String, Object> getBookingSchedule(Integer page, Integer limit, String hoscode, String depcode) {
        Map<String, Object> result = new HashMap<>();
        //获取预约规则
        //根据医院编号获取预约规则
        Hospital hospital = hospitalService.getByHoscode(hoscode);
        if (hospital == null){
            throw new AppointmentRegisterException(ResultCodeEnum.DATA_ERROR);
        }
        BookingRule bookingRule = hospital.getBookingRule();
        //根据预约规则获取可预约日期的数据（分页显示）
        IPage iPage = this.getListDate(page,limit,bookingRule);
        //当前可预约日期
        List<Date> dateList = iPage.getRecords();
        //获取可预约日期里面科室的剩余预约数
        Criteria criteria = Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode)
                .and("workDate").in(dateList);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                //根据workDate分组，求和可预约数量和放号数量
                Aggregation.group("workDate")
                .first("workDate").as("workDate")
                .count().as("docCount")
                .sum("availableNumber").as("availableNumber")
                .sum("reservedNumber").as("reservedNumber")
        );
        AggregationResults<BookingScheduleRuleVo> aggregationResults
                = mongoTemplate.aggregate(aggregation, Schedule.class, BookingScheduleRuleVo.class);

        List<BookingScheduleRuleVo> scheduleRuleVoList = aggregationResults.getMappedResults();

        //合并数据  map集合 key日期 value预约规则和预约数量等
        Map<Date, BookingScheduleRuleVo> scheduleRuleVoMap = new HashMap<>();
        if(!CollectionUtils.isEmpty(scheduleRuleVoList)){
            scheduleRuleVoMap = scheduleRuleVoList.stream().
                    collect(Collectors.toMap(BookingScheduleRuleVo::getWorkDate,
                            BookingScheduleRuleVo -> BookingScheduleRuleVo));
        }

        //获取可预约排班规则
        List<BookingScheduleRuleVo> bookingScheduleRuleVoList = new ArrayList<>();
        for (int i = 0, len = dateList.size(); i < len; i++){
            Date date = dateList.get(i);
            //从map集合中根据key日期获取value值
            BookingScheduleRuleVo bookingScheduleRuleVo = scheduleRuleVoMap.get(date);
            //如果当天没有排班医生
            if(bookingScheduleRuleVo == null){
                bookingScheduleRuleVo = new BookingScheduleRuleVo();
                //就诊医生人数
                bookingScheduleRuleVo.setDocCount(0);
                //科室剩余预约数 -1代表无号
                bookingScheduleRuleVo.setAvailableNumber(-1);
            }
            bookingScheduleRuleVo.setWorkDate(date);
            bookingScheduleRuleVo.setWorkDateMd(date);
            //计算当前预约日期对应的星期
            String dayOfWeek = this.getDayOfWeek(new DateTime(date));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);

            //最后一页最后一条记录为即将预约 状态 0：正常 1：即将放号 -1：当天已停止挂号
            if (i == len-1 && page == iPage.getPages()){
                bookingScheduleRuleVo.setStatus(1);
            }else {
                bookingScheduleRuleVo.setStatus(0);
            }

            //当天预约如果过了停号时间，不能预约
            if (i == 0 && page == 1){
                DateTime stopTime = this.getDateTime(new Date(), bookingRule.getStopTime());
                //如果停止预约时间早于当前时间
                if(stopTime.isBeforeNow()){
                    //停止预约
                    bookingScheduleRuleVo.setStatus(-1);
                }
            }

            bookingScheduleRuleVoList.add(bookingScheduleRuleVo);
        }

        //可预约日期规则数据
        result.put("bookingScheduleList", bookingScheduleRuleVoList);
        result.put("total", iPage.getTotal());
        //其他基础数据
        Map<String, String> baseMap = new HashMap<>();
        //医院名称
        baseMap.put("hosname", hospitalService.getHospName(hoscode));
        //科室
        Department department =departmentService.getDepartment(hoscode, depcode);
        //大科室名称
        baseMap.put("bigname", department.getBigname());
        //科室名称
        baseMap.put("depname", department.getDepname());
        //月
        baseMap.put("workDateString", new DateTime().toString("yyyy年MM月"));
        //放号时间
        baseMap.put("releaseTime", bookingRule.getReleaseTime());
        //停号时间
        baseMap.put("stopTime", bookingRule.getStopTime());
        result.put("baseMap", baseMap);
        return result;

    }

    //根据排班id获取排班数据
    @Override
    public Schedule getByScheduleId(String scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).get();
        return this.packageSchedule(schedule);
    }

    //根据排班id获取预约下单数据
    @Override
    public ScheduleOrderVo getScheduleOrderVo(String scheduleId) {
        ScheduleOrderVo scheduleOrderVo = new ScheduleOrderVo();
        //获取排班信息
        Schedule schedule = baseMapper.selectById(scheduleId);
        if(schedule == null){
            throw new AppointmentRegisterException(ResultCodeEnum.PARAM_ERROR);
        }
        //获取预约规则信息
        Hospital hospital = hospitalService.getByHoscode(schedule.getHoscode());
        if(hospital == null){
            throw new AppointmentRegisterException(ResultCodeEnum.PARAM_ERROR);
        }
        BookingRule bookingRule = hospital.getBookingRule();
        if(bookingRule == null){
            throw new AppointmentRegisterException(ResultCodeEnum.PARAM_ERROR);
        }
        //把获取的数据设置到scheduleOrderVo中
        scheduleOrderVo.setHoscode(schedule.getHoscode());
        scheduleOrderVo.setHosname(hospitalService.getHospName(schedule.getHoscode()));
        scheduleOrderVo.setDepcode(schedule.getDepcode());
        scheduleOrderVo.setDepname(departmentService.getDepName(schedule.getHoscode(), schedule.getDepcode()));
        scheduleOrderVo.setHosScheduleId(schedule.getHosScheduleId());
        scheduleOrderVo.setAvailableNumber(schedule.getAvailableNumber());
        scheduleOrderVo.setTitle(schedule.getTitle());
        scheduleOrderVo.setReserveDate(schedule.getWorkDate());
        scheduleOrderVo.setReserveTime(schedule.getWorkTime());
        scheduleOrderVo.setAmount(schedule.getAmount());

        //退号截止天数（如：就诊前一天为-1，当天为0）
        int quitDay = bookingRule.getQuitDay();
        DateTime quitTime = this.getDateTime(new DateTime(schedule.getWorkDate()).plusDays(quitDay).toDate(), bookingRule.getQuitTime());
        scheduleOrderVo.setQuitTime(quitTime.toDate());

        //预约开始时间
        DateTime startTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        scheduleOrderVo.setStartTime(startTime.toDate());

        //预约截止时间
        DateTime endTime = this.getDateTime(new DateTime().plusDays(bookingRule.getCycle()).toDate(), bookingRule.getStopTime());
        scheduleOrderVo.setEndTime(endTime.toDate());

        //当天停止挂号时间
        DateTime stopTime = this.getDateTime(new Date(), bookingRule.getStopTime());
        scheduleOrderVo.setStopTime(stopTime.toDate());

        return scheduleOrderVo;
    }

    //更新排班信息 用于mq
    @Override
    public void update(Schedule schedule) {
        schedule.setUpdateTime(new Date());
        scheduleRepository.save(schedule);
    }

    //获取可预约日期分页数据
    private IPage getListDate(Integer page, Integer limit, BookingRule bookingRule) {
        //获取当天放号时间 年 月 日 时 分
        DateTime releaseTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        //获取预约周期
        Integer cycle = bookingRule.getCycle();
        //如果放号时间已经过了，那么预约周期从后一天开始计算
        if (releaseTime.isBeforeNow()){
            cycle += 1;
        }
        //获取可预约所有日期，最后一天显示即将放号
        List<Date> dateList = new ArrayList<>();
        for (Integer i = 0; i < cycle; i++) {
            DateTime curDateTime = new DateTime().plusDays(i);
            String dateString = curDateTime.toString("yyyy-MM-dd");
            dateList.add(new DateTime(dateString).toDate());
        }
        //预约周期不同，每页显示日期最多显示7天，超过七天进行分页
        List<Date> pageDateList = new ArrayList<>();
        int start = (page-1)*limit;
        int end = (page-1)*limit+limit;
        //如果可以显示数据小于7，则直接显示，如果大于7，那么进行分页
        if (end > dateList.size()){
            end = dateList.size();
        }
        for (int i = start; i < end; i++) {
            pageDateList.add(dateList.get(i));
        }
        IPage<Date> iPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, 7, dateList.size());
        iPage.setRecords(pageDateList);
        return iPage;
    }

    /**
     * 将Date日期（yyyy-MM-dd HH：mm）转换为DateTime
     * @param date
     * @param timeString
     * @return
     */
    private DateTime getDateTime(Date date, String timeString){
        String dateTimeString = new DateTime(date).toString("yyyy-MM-dd")+" "+timeString;
        DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").parseDateTime(dateTimeString);
        return dateTime;
    }

    //封装排班详情中的其他值
    private Schedule packageSchedule(Schedule schedule){
        //设置医院名称
        schedule.getParam().put("hosname",hospitalService.getHospName(schedule.getHoscode()));
        //设置科室名称
        schedule.getParam().put("depname",departmentService.getDepName(schedule.getHoscode(),schedule.getDepcode()));
        //设置日期对应的日期值
        schedule.getParam().put("dayOfWeek",this.getDayOfWeek(new DateTime(schedule.getWorkDate())));

        return schedule;
    }

    /**
     * 根据日期获取周几数据
     * @param dateTime
     * @return
     */
    private String getDayOfWeek(DateTime dateTime) {
        String dayOfWeek = "";
        switch (dateTime.getDayOfWeek()) {
            case DateTimeConstants.SUNDAY:
                dayOfWeek = "周日";
                break;
            case DateTimeConstants.MONDAY:
                dayOfWeek = "周一";
                break;
            case DateTimeConstants.TUESDAY:
                dayOfWeek = "周二";
                break;
            case DateTimeConstants.WEDNESDAY:
                dayOfWeek = "周三";
                break;
            case DateTimeConstants.THURSDAY:
                dayOfWeek = "周四";
                break;
            case DateTimeConstants.FRIDAY:
                dayOfWeek = "周五";
                break;
            case DateTimeConstants.SATURDAY:
                dayOfWeek = "周六";
            default:
                break;
        }
        return dayOfWeek;
    }
}
