package com.CPG.ar.hosp.repository;

import com.CPG.ar.entity.hosp.Department;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends MongoRepository<Department, String> {
    //查询科室信息
    Department getDepartmentByHoscodeAndDepcode(String hoscode, String depcode);
}
