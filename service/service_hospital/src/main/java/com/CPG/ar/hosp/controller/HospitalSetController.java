package com.CPG.ar.hosp.controller;

import com.CPG.ar.common.exception.AppointmentRegisterException;
import com.CPG.ar.common.result.Result;
import com.CPG.ar.common.utils.MD5;
import com.CPG.ar.entity.hosp.HospitalSet;
import com.CPG.ar.hosp.service.HospitalSetService;
import com.CPG.ar.vo.hosp.HospitalQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@Api(tags = "医院设置管理")
@RestController
@RequestMapping("/admin/hosp/hospitalSet")
public class HospitalSetController {

    //注入service
    @Autowired
    private HospitalSetService hospitalSetService;

    //1 查询医院设置表里的所有信息
    @ApiOperation(value = "获取所有医院设置")
    @GetMapping("findAll")
    public Result findAllHospitalSet(){
        //调用service中的方法
        List<HospitalSet> list = hospitalSetService.list();
        return Result.ok(list);
    }

    //2 逻辑删除医院设置
    @ApiOperation(value = "逻辑删除医院设置信息")
    @DeleteMapping("{id}")
    public Result removeHospital(@PathVariable Long id){
        boolean flag = hospitalSetService.removeById(id);
        return flag?Result.ok():Result.fail();
    }

    //3 条件查询带分页
    @PostMapping("findPageHospSet/{current}/{limit}")
    public Result findPageHospSet(@PathVariable long current,
                                  @PathVariable long limit,
                                  @RequestBody(required = false) HospitalQueryVo hospitalQueryVo){
        //创建page对象，传递当前页，每页记录数
        Page<HospitalSet> page = new Page<>(current, limit);
        //构造条件
        QueryWrapper<HospitalSet> wrapper = new QueryWrapper<>();
        //医院名称
        String hosname = hospitalQueryVo.getHosname();
        //医院编号
        String hoscode = hospitalQueryVo.getHoscode();
        if (StringUtils.hasLength(hosname)){
            wrapper.like("hosname",hospitalQueryVo.getHosname());
        }
        if (StringUtils.hasLength(hoscode)){
            wrapper.eq("hoscode",hospitalQueryVo.getHoscode());
        }
        //调用方法实现分页查询
        Page<HospitalSet> pageHostpitalSet = hospitalSetService.page(page, wrapper);
        //返回结果
        return Result.ok(pageHostpitalSet);
    }

    //4 添加医院设置
    @PostMapping("saveHospitalSet")
    public Result saveHospitalSet(@RequestBody HospitalSet hospitalSet){
        //设置状态 1：可以使用 0：不能使用
        hospitalSet.setStatus(1);
        //签名秘钥
        Random random = new Random();
        hospitalSet.setSignKey(MD5.encrypt(System.currentTimeMillis()+""+random.nextInt(1000)));
        //调用service
        boolean save = hospitalSetService.save(hospitalSet);
        return save?Result.ok():Result.fail();

    }

    //5 根据id获取医院设置
    @GetMapping("getHospitalSet/{id}")
    public Result getHospitalSet(@PathVariable long id){
        HospitalSet hospitalSet = hospitalSetService.getById(id);
        return Result.ok(hospitalSet);
    }

    //6 修改医院设置
    @PostMapping("updateHospitalSet")
    public Result updateHospitalSet(@RequestBody HospitalSet hospitalSet){
        boolean flag = hospitalSetService.updateById(hospitalSet);
        return flag?Result.ok():Result.fail();
    }

    //7 批量删除医院设置
    @DeleteMapping("batchRemove")
    public Result batchRemoveHospitalSet(@RequestBody List<Long> idList){
        hospitalSetService.removeByIds(idList);
        return Result.ok();
    }

    //8 医院设置锁定和解锁
    @PutMapping("lockHospitalSet/{id}/{status}")
    public Result lockHospitalSet(@PathVariable long id,
                                  @PathVariable Integer status){
        //根据id查询出医院设置的信息
        HospitalSet hospitalSet = hospitalSetService.getById(id);
        //设置医院状态
        hospitalSet.setStatus(status);
        //调用方法
        hospitalSetService.updateById(hospitalSet);
        return Result.ok();
    }

    //9 发送签名秘钥
    @PutMapping("sendKey/{id}")
    public Result sendKeyHospitalSet(@PathVariable long id){
        HospitalSet hospitalSet = hospitalSetService.getById(id);
        String signKey = hospitalSet.getSignKey();
        String hoscode = hospitalSet.getHoscode();
        String hosname = hospitalSet.getHosname();
        //TODO 发送短信
        return Result.ok();
    }

}
