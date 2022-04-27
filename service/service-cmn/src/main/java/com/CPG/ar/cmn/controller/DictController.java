package com.CPG.ar.cmn.controller;

import com.CPG.ar.cmn.service.DictService;
import com.CPG.ar.common.result.Result;
import com.CPG.ar.entity.cmn.Dict;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@Api(tags = "数据字典的接口")
@CrossOrigin
@RequestMapping("/admin/cmn/dict")
public class DictController {

    @Autowired
    private DictService dictService;

    /**
     * 根据数据id查询子数据列表
     * @param id
     * @return
     */
    @ApiOperation(value = "根据数据id查询子数据列表")
    @GetMapping("findChildData/{id}")
    public Result findChildData(@PathVariable Long id){
        List<Dict> dictList = dictService.findChildData(id);
        return Result.ok(dictList);
    }

    /**
     * 导出数据字典接口
     * @param response
     * @return
     */
    @GetMapping("exportData")
    public void exportDict(HttpServletResponse response){
        dictService.exportDictData(response);
    }

    /**
     * 导入数据字典接口
     * @param file
     * @return
     */
    @PostMapping("importData")
    public Result importDict(MultipartFile file){
        dictService.importDictData(file);
        return Result.ok();
    }
}
