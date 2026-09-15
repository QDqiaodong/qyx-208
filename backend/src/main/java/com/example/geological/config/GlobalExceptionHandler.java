package com.example.geological.config;

import com.example.geological.dto.ResponseDTO;
import com.example.geological.exception.MemberDepartedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 队员编号存在但已停用（已离队）：按队员查资产、改挂、编辑一律拒绝。
     * 明确报「已离队」，与编号不存在（400）区分开，且不附带任何原小队资产数据。
     */
    @ExceptionHandler(MemberDepartedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseDTO<Void> handleMemberDeparted(MemberDepartedException e) {
        log.warn("Member departed: {}", e.getMessage());
        return ResponseDTO.error(409, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseDTO<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ResponseDTO.error(400, e.getMessage());
    }

    /**
     * 唯一约束兜底：样品袋「同小队+同送检日+同袋号」并发时，
     * 若唯一索引冲突在 Service 捕获之外冒出，仍返回明确的 400 业务错误，
     * 而不是笼统的 500。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseDTO<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMessage());
        return ResponseDTO.error(400, "登记冲突：该小队、该送检日、该袋号已存在登记（同袋已在途/办结），本次提交失败，不允许重复落账");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseDTO<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", message);
        return ResponseDTO.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseDTO<Void> handleException(Exception e) {
        log.error("Unexpected error: ", e);
        return ResponseDTO.error("服务器内部错误");
    }
}