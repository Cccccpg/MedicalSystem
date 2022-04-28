package com.CPG.ar.cmn.service.impl;

import com.CPG.ar.cmn.listener.DictListener;
import com.CPG.ar.cmn.mapper.DictMapper;
import com.CPG.ar.cmn.service.DictService;
import com.CPG.ar.entity.cmn.Dict;
import com.CPG.ar.vo.cmn.DictEeVo;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService{


    //根据数据id查询子数据列表
    @Override
    @Cacheable(value = "dict",keyGenerator = "keyGenerator")
    public List<Dict> findChildData(Long id) {
        QueryWrapper<Dict> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id",id);
        //ServiceImpl中已经帮忙注入了，所以直接调用就行
        List<Dict> dictList = baseMapper.selectList(wrapper);
        //向list集合每个dict对象中设置hasChildren
        for (Dict dict : dictList) {
            Long dictId = dict.getId();
            boolean hasChildren = this.hasChildren(dictId);
            dict.setHasChildren(hasChildren);
        }
        return dictList;
    }

    //导出数据字典实现
    @Override
    public void exportDictData(HttpServletResponse response) {
        //设置类型
        response.setContentType("application/vnd.ms-excel");
        //设置编码
        response.setCharacterEncoding("utf-8");
        //这里使用URLEncode.encode可以防止中文乱码，当然和easyExcel没有关系
        String fileName = "dict";
        response.setHeader("Content-disposition","attachment;filename="+fileName+".xlsx");

        //查询数据库
        List<Dict> dictList = baseMapper.selectList(null);
        //把Dict转换成DictEeVo
        List<DictEeVo> dictEeVoList = new ArrayList<>();
        for (Dict dict : dictList) {
            DictEeVo dictEeVo = new DictEeVo();
            BeanUtils.copyProperties(dict,dictEeVo);
            dictEeVoList.add(dictEeVo);
        }
        //调用方法进行写的操作
        try {
            EasyExcel.write(response.getOutputStream(), DictEeVo.class).sheet("dict")
                    .doWrite(dictEeVoList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //导入数据字典实现
    @Override
    @CacheEvict(value = "dict", allEntries=true)
    public void importDictData(MultipartFile file) {
        try {
            EasyExcel.read(file.getInputStream(),DictEeVo.class,new DictListener(baseMapper)).sheet().doRead();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据dictCode查询dict对象
     * @param dictCode
     * @return
     */
    public Dict getDictByDictCode(String dictCode){
        QueryWrapper<Dict> dictQueryWrapperWrapper = new QueryWrapper<>();
        dictQueryWrapperWrapper.eq("dict_code",dictCode);
        Dict dict = baseMapper.selectOne(dictQueryWrapperWrapper);
        return dict;
    }

    //根据dictCode和value查询
    @Override
    public String getDictName(String dictCode, String value) {
        //如果dictCode值为空，直接根据value查询
        if (StringUtils.isEmpty(dictCode)) {
            QueryWrapper<Dict> dictQueryWrapperWrapper = new QueryWrapper<>();
            dictQueryWrapperWrapper.eq("value",value);
            Dict dict = baseMapper.selectOne(dictQueryWrapperWrapper);
            return dict.getName();
        }else {
            Dict dict_code = this.getDictByDictCode(dictCode);
            Long parent_id = dict_code.getId();
            //根据parent_id和value进行查询
            Dict finalDict = baseMapper.selectOne(new QueryWrapper<Dict>()
                    .eq("parent_id",parent_id)
                    .eq("value",value));
            return finalDict.getName();
        }
    }

    /**
     * 根据dictCode查询下级节点
     * @param dictCode
     * @return
     */
    @Override
    public List<Dict> findByDictCode(String dictCode) {
        //根据dictCode获取对应的id
        Dict dictByDictCode = this.getDictByDictCode(dictCode);
        //根据id获取子节点
        List<Dict> childData = this.findChildData(dictByDictCode.getId());
        return childData;
    }

    //判断id下面是否有子节点
    private boolean hasChildren(Long id){
        QueryWrapper<Dict> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id",id);
        Integer count = baseMapper.selectCount(wrapper);
        return count>0;
    }
}
