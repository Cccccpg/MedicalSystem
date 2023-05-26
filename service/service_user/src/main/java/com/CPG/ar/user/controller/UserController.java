package com.CPG.ar.user.controller;

import com.CPG.ar.common.result.Result;
import com.CPG.ar.entity.user.UserInfo;
import com.CPG.ar.user.service.UserInfoService;
import com.CPG.ar.vo.user.UserInfoQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "平台用户管理接口")
@RestController
@RequestMapping("/admin/user")
public class UserController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 用户列表（条件查询带分页）
     * @param page
     * @param limit
     * @param userInfoQueryVo
     * @return
     */
    @ApiOperation(value = "用户列表接口")
    @GetMapping("{page}/{limit}")
    public Result userList(@PathVariable Long page,
                           @PathVariable Long limit,
                           UserInfoQueryVo userInfoQueryVo){
        Page<UserInfo> pageParam = new Page<>(page,limit);
        IPage<UserInfo> pageModel = userInfoService.selectPage(pageParam,userInfoQueryVo);
        return Result.ok(pageModel);
    }

    /**
     * 锁定用户
     * @param userId
     * @param status
     * @return
     */
    @ApiOperation(value = "用户锁定接口")
    @GetMapping("lock/{userId}/{status}")
    public Result lock(@PathVariable Long userId,
                       @PathVariable Integer status){
        userInfoService.lock(userId,status);
        return Result.ok();
    }

    /**
     * 用户详情
     * @param userId
     * @return
     */
    @ApiOperation(value = "用户详情接口")
    @GetMapping("show/{userId}")
    public Result show(@PathVariable Long userId){
        Map<String, Object> map = userInfoService.show(userId);
        return Result.ok(map);
    }

    /**
     * 审批列表（条件查询带分页）
     * @param page
     * @param limit
     * @param userInfoQueryVo
     * @return
     */
    @ApiOperation(value = "审批列表接口")
    @GetMapping("authList/{page}/{limit}")
    public Result authList(@PathVariable Long page,
                           @PathVariable Long limit,
                           UserInfoQueryVo userInfoQueryVo){
        Page<UserInfo> pageParam = new Page<>(page,limit);
        IPage<UserInfo> pageModel = userInfoService.selectAuthPage(pageParam,userInfoQueryVo);
        return Result.ok(pageModel);
    }
    /**
     * 认证审批
     * @param userId
     * @param authStatus
     * @return
     */
    @ApiOperation(value = "认证审批接口")
    @GetMapping("approval/{userId}/{authStatus}")
    public Result approval(@PathVariable Long userId, @PathVariable Integer authStatus){
        userInfoService.approval(userId,authStatus);
        return Result.ok();
    }
}
