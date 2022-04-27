package com.CPG.ar.cmn.service;

import com.CPG.ar.entity.cmn.Dict;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface DictService extends IService<Dict> {

    //根据id查询数据
    List<Dict> findChildData(Long id);

    //导出数据字典接口
    void exportDictData(HttpServletResponse response);

    //导入数据字典接口
    void importDictData(MultipartFile file);
}
