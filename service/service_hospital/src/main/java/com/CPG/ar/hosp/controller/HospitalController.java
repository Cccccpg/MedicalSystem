package com.CPG.ar.hosp.controller;

import com.CPG.ar.common.result.Result;
import com.CPG.ar.entity.hosp.Hospital;
import com.CPG.ar.hosp.service.HospitalService;
import com.CPG.ar.vo.hosp.HospitalQueryVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("admin/hosp/hospital")
@CrossOrigin
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    /**
     * 医院列表
     * @param page
     * @param limit
     * @param hospitalQueryVo
     * @return
     */
    @GetMapping("list/{page}/{limit}")
    public Result listHospital(@PathVariable Integer page,
                               @PathVariable Integer limit,
                               HospitalQueryVo hospitalQueryVo){
        Page<Hospital> pageModel = hospitalService.selectHospPage(page,limit,hospitalQueryVo);
        return Result.ok(pageModel);
    }

    /**
     * 更新医院上线状态
     * @param id
     * @param status
     * @return
     */
    @ApiOperation(value = "更新医院上线状态")
    @GetMapping("updateHospitalStatus/{id}/{status}")
    public Result updateHospitalStatus(@PathVariable String id,
                                       @PathVariable Integer status){
        hospitalService.updateHospitalStatus(id,status);
        return Result.ok();
    }

    /**
     * 获取医院详细信息
     * @param id
     * @return
     */
    @ApiOperation(value = "医院详细信息")
    @GetMapping("showHospitalDetail/{id}")
    public Result showHospitalDetail(@PathVariable String id){
        Map<String, Object> hospitalMap = hospitalService.getHospitalById(id);
        return Result.ok(hospitalMap);
    }
}
