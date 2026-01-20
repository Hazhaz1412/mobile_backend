package com.react.mobile.Config;

import com.react.mobile.DTO.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Controller
public class CustomErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;
    
    @Value("${app.debug:false}")
    private boolean debugMode;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request, WebRequest webRequest) {
        
        // Lấy thông tin lỗi
        Map<String, Object> errorAttributesMap = errorAttributes.getErrorAttributes(
            webRequest, 
            debugMode ? ErrorAttributeOptions.of(
                ErrorAttributeOptions.Include.STACK_TRACE,
                ErrorAttributeOptions.Include.EXCEPTION,
                ErrorAttributeOptions.Include.MESSAGE,
                ErrorAttributeOptions.Include.BINDING_ERRORS
            ) : ErrorAttributeOptions.defaults()
        );
        
        Integer status = (Integer) errorAttributesMap.get("status");
        String error = (String) errorAttributesMap.get("error");
        String message = (String) errorAttributesMap.get("message");
        String path = (String) errorAttributesMap.get("path");
        
        // Build response
        ErrorResponse.ErrorResponseBuilder responseBuilder = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status != null ? status : 500)
                .error(error != null ? error : "Internal Server Error")
                .path(path);
        
        // Nếu DEBUG = true, thêm chi tiết
        if (debugMode) {
            responseBuilder
                .message(message)
                .exception((String) errorAttributesMap.get("exception"))
                .trace((String) errorAttributesMap.get("trace"))
                .details(errorAttributesMap);
            
            log.error("Error occurred: {} - {} at {}", status, message, path);
        } else {
            // Nếu DEBUG = false, chỉ trả message đơn giản
            responseBuilder.message(getSimpleMessage(status));
        }
        
        ErrorResponse errorResponse = responseBuilder.build();
        
        HttpStatus httpStatus = HttpStatus.valueOf(status != null ? status : 500);
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }
    
    private String getSimpleMessage(Integer status) {
        if (status == null) return "An error occurred";
        
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Resource Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            case 503 -> "Service Unavailable";
            default -> "Error";
        };
    }
}
