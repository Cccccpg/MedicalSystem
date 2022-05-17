package com.CPG.ar.user.service.impl;

import com.CPG.ar.cmn.client.DictFeignClient;
import com.CPG.ar.entity.user.Patient;
import com.CPG.ar.enums.DictEnum;
import com.CPG.ar.user.mapper.PatientMapper;
import com.CPG.ar.user.service.PatientService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements PatientService {

    @Autowired
    private DictFeignClient dictFeignClient;

    //获取就诊人列表
    @Override
    public List<Patient> findAllByUserId(Long userId) {
        //根据userid查询所有就诊人信息列表
        QueryWrapper<Patient> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);
        List<Patient> patientList = baseMapper.selectList(queryWrapper);
        //通过远程调用，查询数据字典表，得到patientList中编码具体的含义
        patientList.stream().forEach(item -> {
            //其他参数的封装
            this.packPatient(item);
        });
        return patientList;
    }

    //根据id获取就诊人信息
    @Override
    public Patient getPatientId(Long id) {
        Patient patient = baseMapper.selectById(id);
        return this.packPatient(patient);
    }

    //Patient对象中其他参数封装
    private Patient packPatient(Patient patient) {
        //根据证件编码，获取证件类型具体值
        String certificatesTypeString =
                dictFeignClient.getName(DictEnum.CERTIFICATES_TYPE.getDictCode(), patient.getCertificatesType());
        //联系人的证件类型
        String contactsCertificatesTypeString =
                dictFeignClient.getName(DictEnum.CERTIFICATES_TYPE.getDictCode(),patient.getContactsCertificatesType());
        //省
        String provinceString = dictFeignClient.getName(patient.getProvinceCode());
        //市
        String cityString = dictFeignClient.getName(patient.getCityCode());
        //区
        String districtString = dictFeignClient.getName(patient.getDistrictCode());

        patient.getParam().put("certificatesTypeString", certificatesTypeString);
        patient.getParam().put("contactsCertificatesTypeString", contactsCertificatesTypeString);
        patient.getParam().put("provinceString", provinceString);
        patient.getParam().put("cityString", cityString);
        patient.getParam().put("districtString", districtString);
        patient.getParam().put("fullAddress", provinceString + cityString + districtString + patient.getAddress());

        return patient;
    }
}
