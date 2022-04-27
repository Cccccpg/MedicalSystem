package com.CPG.ar.hosp.controller;

import com.CPG.ar.common.result.Result;
import com.CPG.ar.entity.hosp.Hospital;
import com.CPG.ar.hosp.service.HospitalService;
import com.CPG.ar.vo.hosp.HospitalQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/hosp/hospital")
@CrossOrigin
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    //医院列表
    @GetMapping("list/{page}/{limit}")
    public Result listHospital(@PathVariable Integer page,
                               @PathVariable Integer limit,
                               HospitalQueryVo hospitalQueryVo){
        Page<Hospital> pageModel = hospitalService.selectHospPage(page,limit,hospitalQueryVo);
        return Result.ok(pageModel);
    }
}
