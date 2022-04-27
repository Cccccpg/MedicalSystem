package com.CPG.ar.common.exception;

import com.CPG.ar.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //全局异常处理
    @ExceptionHandler(Exception.class)
    public Result error(Exception e){
        e.printStackTrace();
        return Result.fail();
    }

    //自定义异常处理
    @ExceptionHandler(AppointmentRegisterException.class)
    public Result error(AppointmentRegisterException e){
        e.printStackTrace();
        return Result.fail();
    }
}
