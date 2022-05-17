package com.CPG.ar.user.service;

import com.CPG.ar.entity.user.Patient;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PatientService extends IService<Patient> {

    //获取就诊人列表
    List<Patient> findAllByUserId(Long userId);

    //根据id获取就诊人信息
    Patient getPatientId(Long id);
}
