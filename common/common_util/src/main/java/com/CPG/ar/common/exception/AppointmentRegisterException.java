package com.CPG.ar.common.exception;

import com.CPG.ar.common.result.ResultCodeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 自定义全局异常类
 */
@Data
@ApiModel(value = "自定义全局异常类")
public class AppointmentRegisterException extends RuntimeException {

    @ApiModelProperty(value = "异常状态码")
    private Integer code;

    /**
     * 通过状态码和错误消息创建异常对象
     * @param message
     * @param code
     */
    public AppointmentRegisterException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    public AppointmentRegisterException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }

    @Override
    public String toString() {
        return "AppointmentRegisterException{" +
                "code=" + code +
                ", message=" + this.getMessage() +
                '}';
    }
}
