package com.CPG.easyexcel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import java.util.Map;

public class ExcelListener extends AnalysisEventListener<UserData> {

    //一行一行读取excel的内容，从第二行读取，因为第一行是表头
    @Override
    public void invoke(UserData userData, AnalysisContext analysisContext) {
        System.out.println(userData);
    }

    @Override
    public void invokeHeadMap(Map<Integer,String> headMap, AnalysisContext analysisContext){
        System.out.println("表头信息："+headMap);
    }

    //读取之后会执行
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }
}
