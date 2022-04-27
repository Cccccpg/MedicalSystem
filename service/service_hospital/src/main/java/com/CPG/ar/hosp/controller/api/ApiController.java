package com.CPG.ar.hosp.controller.api;

import com.CPG.ar.common.exception.AppointmentRegisterException;
import com.CPG.ar.common.helper.HttpRequestHelper;
import com.CPG.ar.common.result.Result;
import com.CPG.ar.common.result.ResultCodeEnum;
import com.CPG.ar.common.utils.MD5;
import com.CPG.ar.entity.hosp.Department;
import com.CPG.ar.entity.hosp.Hospital;
import com.CPG.ar.entity.hosp.Schedule;
import com.CPG.ar.hosp.service.DepartmentService;
import com.CPG.ar.hosp.service.HospitalService;
import com.CPG.ar.hosp.service.HospitalSetService;
import com.CPG.ar.hosp.service.ScheduleService;
import com.CPG.ar.vo.hosp.DepartmentQueryVo;
import com.CPG.ar.vo.hosp.ScheduleQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/hosp")
public class ApiController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private HospitalSetService hospitalSetService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ScheduleService scheduleService;

    /**
     * 获取传递信息的工具
     * @param request
     * @return
     */
    public Map<String, Object> translateUtile(HttpServletRequest request){
        //获取传递过来的医院信息
        Map<String, String[]> requestMap = request.getParameterMap();
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(requestMap);
        //获取医院系统传递过来的签名,签名进行了MD5加密
        String hospSign = (String)paramMap.get("sign");
        //根据传递过来的医院编号，查询数据库，查询签名
        String hoscode = (String)paramMap.get("hoscode");
        String signKey = hospitalSetService.getSignKey(hoscode);
        //把数据库查出来的签名进行加密
        String signKeyMd5 = MD5.encrypt(signKey);
        //判断签名是否一致
        if (!hospSign.equals(signKeyMd5)){
            throw new AppointmentRegisterException(ResultCodeEnum.SIGN_ERROR);
        }
        return paramMap;
    }

    //上传医院接口
    @PostMapping("saveHospital")
    public Result saveHospital(HttpServletRequest request){
        Map<String, Object> paramMap = translateUtile(request);
        //传输过程中”+“转换为了” “，因此我们要转化回来
        String logoData = (String)paramMap.get("logoData");
        logoData = logoData.replaceAll(" ","+");
        paramMap.put("logoData",logoData);

        //调用service的方法
        hospitalService.save(paramMap);
        return Result.ok();
    }

    //查询医院
    @PostMapping("hospital/show")
    public Result getHospital(HttpServletRequest request){
        //调用Service方法实现根据医院编号查询
        Hospital hospital = hospitalService.getByHoscode((String)translateUtile(request).get("hoscode"));
        return Result.ok(hospital);
    }

    //上传科室接口
    @PostMapping("saveDepartment")
    public Result saveDepartment(HttpServletRequest request){
        //调用service中方法
        departmentService.save(translateUtile(request));
        return Result.ok();
    }

    //查询科室接口
    @PostMapping("department/list")
    public Result findDepartment(HttpServletRequest request){
        int page = StringUtils.isEmpty(translateUtile(request).get("page")) ? 1 : Integer.parseInt((String)translateUtile(request).get("page"));
        int limit = StringUtils.isEmpty(translateUtile(request).get("limit")) ? 1 : Integer.parseInt((String)translateUtile(request).get("limit"));
        DepartmentQueryVo departmentQueryVo = new DepartmentQueryVo();
        departmentQueryVo.setHoscode((String)translateUtile(request).get("hoscode"));
        //调用service方法
        Page<Department> pageModel = departmentService.findPageDepartment(page,limit,departmentQueryVo);
        return Result.ok(pageModel);
    }

    //删除科室接口
    @PostMapping("department/remove")
    public Result departmentRemove(HttpServletRequest request){
        departmentService.remove((String)translateUtile(request).get("hoscode"),(String)translateUtile(request).get("depcode"));
        return Result.ok();
    }

    //上传排班接口
    @PostMapping("saveSchedule")
    public Result saveSchedule(HttpServletRequest request){
        scheduleService.save(translateUtile(request));
        return Result.ok();
    }

    //查询排班接口
    @PostMapping("schedule/list")
    public Result findSchedule(HttpServletRequest request){
        int page = StringUtils.isEmpty(translateUtile(request).get("page")) ? 1 : Integer.parseInt((String)translateUtile(request).get("page"));
        int limit = StringUtils.isEmpty(translateUtile(request).get("limit")) ? 1 : Integer.parseInt((String)translateUtile(request).get("limit"));
        ScheduleQueryVo scheduleQueryVo = new ScheduleQueryVo();
        scheduleQueryVo.setHoscode((String)translateUtile(request).get("hoscode"));
        scheduleQueryVo.setDepcode((String)translateUtile(request).get("depcode"));
        //调用service方法，实现查询排班
        Page<Schedule> pageModel = scheduleService.findPageSchedule(page,limit,scheduleQueryVo);
        return Result.ok(pageModel);
    }

    //删除排班接口
    @PostMapping("schedule/remove")
    public Result removeSchedule(HttpServletRequest request){
        scheduleService.remove((String)translateUtile(request).get("hoscode"),(String)translateUtile(request).get("hosScheduleId"));
        return Result.ok();
    }

}
