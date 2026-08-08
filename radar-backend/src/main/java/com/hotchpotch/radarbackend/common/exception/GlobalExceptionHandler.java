package com.hotchpotch.radarbackend.common.exception;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.validation.method.ParameterValidationResult;

import com.hotchpotch.radarbackend.common.response.ApiResponse;
import com.hotchpotch.radarbackend.common.response.ValidationError;

/**
 * 全局异常处理器，将后端异常转换为统一 JSON 响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理可预期的业务异常。
     *
     * @param exception 业务异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return buildResponse(errorCode, exception.getMessage(), null);
    }

    /**
     * 处理请求体对象字段校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        return buildResponse(
                ErrorCode.PARAMETER_ERROR,
                "请求参数校验失败",
                toValidationErrors(exception.getBindingResult()));
    }

    /**
     * 处理表单或模型绑定校验异常。
     *
     * @param exception 绑定异常
     * @return 统一错误响应
     */
    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ResponseEntity<ApiResponse<?>> handleBindException(
            org.springframework.validation.BindException exception) {
        return buildResponse(
                ErrorCode.PARAMETER_ERROR,
                "请求参数校验失败",
                toValidationErrors(exception.getBindingResult()));
    }

    /**
     * 处理方法参数级校验异常。
     *
     * @param exception 方法参数校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception) {
        List<ValidationError> errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ValidationError(
                                parameterName(result),
                                defaultMessage(error.getDefaultMessage()))))
                .toList();
        return buildResponse(ErrorCode.PARAMETER_ERROR, "请求参数校验失败", errors);
    }

    /**
     * 处理普通 Bean Validation 约束异常。
     *
     * @param exception 约束校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolationException(
            ConstraintViolationException exception) {
        List<ValidationError> errors = exception.getConstraintViolations().stream()
                .map(this::toValidationError)
                .toList();
        return buildResponse(ErrorCode.PARAMETER_ERROR, "请求参数校验失败", errors);
    }

    /**
     * 处理请求体无法解析的异常。
     *
     * @param exception 请求体解析异常
     * @return 统一错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        return buildResponse(ErrorCode.PARAMETER_ERROR, "请求体格式错误或无法解析", null);
    }

    /**
     * 处理缺少请求参数的异常。
     *
     * @param exception 缺少请求参数异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception) {
        return buildResponse(ErrorCode.PARAMETER_ERROR, "缺少必要请求参数", null);
    }

    /**
     * 处理请求参数类型转换异常。
     *
     * @param exception 参数类型转换异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception) {
        return buildResponse(ErrorCode.PARAMETER_ERROR, "请求参数类型错误", null);
    }

    /**
     * 处理异步请求客户端主动断开。
     *
     * <p>SSE 客户端刷新页面、关闭页面或网络断开时，Servlet 容器可能在服务端写心跳期间
     * 抛出该异常。此时响应已经不可写，不能再套用统一 JSON 响应，否则会产生二次的
     * {@code HttpMessageNotWritableException}。</p>
     *
     * @param exception 异步请求不可用异常
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException exception) {
        log.debug("SSE 客户端已断开，结束当前异步请求");
    }

    /**
     * 兜底处理未预期的系统异常，避免将内部实现细节返回给调用方。
     *
     * @param exception 未预期异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception exception) {
        log.error("未处理的系统异常", exception);
        return buildResponse(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage(), null);
    }

    private ResponseEntity<ApiResponse<?>> buildResponse(ErrorCode errorCode, String message, Object data) {
        ApiResponse<?> response = ApiResponse.of(errorCode.getCode(), message, data);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    private List<ValidationError> toValidationErrors(BindingResult bindingResult) {
        List<ValidationError> errors = new ArrayList<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.add(new ValidationError(fieldError.getField(), defaultMessage(fieldError.getDefaultMessage())));
        }
        for (ObjectError objectError : bindingResult.getGlobalErrors()) {
            errors.add(new ValidationError(objectError.getObjectName(), defaultMessage(objectError.getDefaultMessage())));
        }
        return errors;
    }

    private ValidationError toValidationError(ConstraintViolation<?> violation) {
        return new ValidationError(
                violation.getPropertyPath().toString(),
                defaultMessage(violation.getMessage()));
    }

    private String parameterName(ParameterValidationResult result) {
        String parameterName = result.getMethodParameter().getParameterName();
        return parameterName == null || parameterName.isBlank() ? "request" : parameterName;
    }

    private String defaultMessage(String message) {
        return message == null || message.isBlank() ? "参数校验失败" : message;
    }
}
