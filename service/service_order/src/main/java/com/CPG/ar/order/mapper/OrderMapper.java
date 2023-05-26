package com.CPG.ar.order.mapper;

import com.CPG.ar.entity.order.OrderInfo;
import com.CPG.ar.vo.order.OrderCountQueryVo;
import com.CPG.ar.vo.order.OrderCountVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderMapper extends BaseMapper<OrderInfo> {

    //查询预约统计数据的方法
    List<OrderCountVo> selectOrderCount(@Param("vo")OrderCountQueryVo orderCountQueryVo);
}
