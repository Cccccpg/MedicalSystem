package com.CPG.ar.hosp.controller.api;

import com.CPG.ar.common.result.Result;
import com.CPG.ar.entity.hosp.Hospital;
import com.CPG.ar.hosp.service.DepartmentService;
import com.CPG.ar.hosp.service.HospitalService;
import com.CPG.ar.vo.hosp.DepartmentVo;
import com.CPG.ar.vo.hosp.HospitalQueryVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hosp/hospital")
public class HospApiController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

    /**
     * 查询医院列表
     * @param page
     * @param limit
     * @param hospitalQueryVo
     * @return
     */
    @ApiOperation(value = "查询医院列表")
    @GetMapping("findHospList/{page}/{limit}")
    public Result findHospList(@PathVariable Integer page,
                               @PathVariable Integer limit,
                               HospitalQueryVo hospitalQueryVo){
        Page<Hospital> hospitals = hospitalService.selectHospPage(page, limit, hospitalQueryVo);
        List<Hospital> content = hospitals.getContent();
        int totalPages = hospitals.getTotalPages();
        return Result.ok(hospitals);
    }

    /**
     * 根据医院名称模糊查询
     * @param hosname
     * @return
     */
    @ApiOperation(value = "根据医院名称模糊查询")
    @GetMapping("findByHosName/{hosname}")
    public Result findByHosName(@PathVariable String hosname){
        List<Hospital> list = hospitalService.findByHosName(hosname);
        return Result.ok(list);
    }

    /**
     * 根据医院编号得到相关科室信息
     * @param hoscode
     * @return
     */
    @ApiOperation(value = "根据医院编号获取科室信息")
    @GetMapping("department/{hoscode}")
    public Result index(@PathVariable String hoscode){
        List<DepartmentVo> list = departmentService.findDeptTree(hoscode);
        return Result.ok(list);
    }

    /**
     * 根据医院编号获取预约挂号详情
     * @param hoscode
     * @return
     */
    @ApiOperation(value = "根据医院编号获取医院预约挂号详情")
    @GetMapping("findHospDetail/{hoscode}")
    public Result findHospDetail(@PathVariable String hoscode){
        Map<String, Object> map = hospitalService.findHospDetail(hoscode);
        return Result.ok(map);
    }
}
