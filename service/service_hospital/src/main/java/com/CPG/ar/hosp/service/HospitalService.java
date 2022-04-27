package com.CPG.ar.hosp.service;

import com.CPG.ar.entity.hosp.Hospital;
import com.CPG.ar.vo.hosp.HospitalQueryVo;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface HospitalService {
    //上传医院接口
    void save(Map<String, Object> paramMap);

    //根据医院编号查询医院
    Hospital getByHoscode(String hoscode);

    //医院列表（条件查询带分页）
    Page<Hospital> selectHospPage(Integer page, Integer limit, HospitalQueryVo hospitalQueryVo);
}
