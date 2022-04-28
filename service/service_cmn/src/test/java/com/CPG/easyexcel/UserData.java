package com.CPG.easyexcel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class UserData {

    @ColumnWidth(20)    //设置列宽
    @ExcelProperty(value = "用户编号",index = 0)
    private int uid;

    @ColumnWidth(20)
    @ExcelProperty(value = "用户名称",index = 1)
    private String username;

}
