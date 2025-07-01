package com.example.washgo.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalTime;

@Data
public class ScheduleInputDTO {

    private Long id;

    /**
     * Giờ mở cửa (định dạng "HH:mm:ss").
     * Khi nhận từ client, Spring sẽ tự parse JSON string thành LocalTime.
     */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime availableFrom;

    /**
     * Giờ đóng cửa (định dạng "HH:mm:ss").
     */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime availableTo;

    /**
     * Sức chứa tối đa của carwash (phải >= 1).
     */
    private Integer capacity;

    /**
     * Cờ này chỉ dùng để trả về cho client biết hiện carwash có đang mở hay không.
     * Client không cần phải set giá trị này khi tạo hoặc cập nhật schedule; server sẽ tự tính.
     */
    private Boolean isActive;

    /**
     * ID của CarwashProfile mà schedule này thuộc về.
     * Khi tạo mới (POST), client phải gửi giá trị này.
     */
    private Long carwashId;
}
