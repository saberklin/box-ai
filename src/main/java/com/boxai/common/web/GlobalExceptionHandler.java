package com.boxai.common.web;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 * 统一处理所有Controller抛出的异常，返回标准格式的ApiResponse
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数验证异常
     * @param ex 方法参数验证异常
     * @return 标准错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("参数验证失败: {}", message);
        return ApiResponse.error(400, "参数验证失败: " + message);
    }

    /**
     * 处理约束验证异常
     * @param ex 约束验证异常
     * @return 标准错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException ex) {
        log.warn("约束验证失败: {}", ex.getMessage());
        return ApiResponse.error(400, "参数格式错误: " + ex.getMessage());
    }

    /**
     * 处理参数类型转换异常
     * @param ex 参数类型不匹配异常
     * @return 标准错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("参数类型错误: {} 应为 {}", ex.getValue(), ex.getRequiredType().getSimpleName());
        return ApiResponse.error(400, "参数类型错误，请检查参数格式");
    }

    /**
     * 处理数据完整性异常（如外键约束、唯一约束等）
     * @param ex 数据完整性异常
     * @return 标准错误响应
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("数据完整性异常: {}", ex.getMessage());
        return ApiResponse.error(400, "数据操作失败，请检查数据完整性");
    }

    /**
     * 处理资源未找到异常
     * @param ex 资源未找到异常
     * @return 标准错误响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleResourceNotFound(NoResourceFoundException ex) {
        log.warn("资源未找到: {}", ex.getResourcePath());
        return ApiResponse.error(404, "请求的资源不存在");
    }

    /**
     * 处理业务异常
     * @param ex 运行时异常
     * @return 标准错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleRuntimeException(RuntimeException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return ApiResponse.error(400, ex.getMessage());
    }

    /**
     * 处理其他未知异常
     * @param ex 通用异常
     * @return 标准错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleOther(Exception ex) {
        log.error("系统异常: ", ex);
        return ApiResponse.error(500, "系统内部错误，请稍后重试");
    }
}


