package com.example.washgo.mapper;

import com.example.washgo.dtos.ScheduleInputDTO;
import com.example.washgo.model.CarwashProfile;
import com.example.washgo.model.Schedule;
import com.example.washgo.service.CarwashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class ScheduleMapper {
    @Autowired
    CarwashService carwashService;


    public  Schedule toSchedule(ScheduleInputDTO scheduleInputDTO){
        Schedule schedule = new Schedule();


        CarwashProfile carwashProfile = carwashService.findCarwashByUserId(scheduleInputDTO.getCarwashId());


        schedule.setActive(scheduleInputDTO.getIsActive());
        if (scheduleInputDTO.getCapacity() < 1) {
            throw new IllegalArgumentException("capacity phải >= 1.");
        }
        schedule.setCapacity(scheduleInputDTO.getCapacity());        // Validate giờ mở/đóng
        LocalTime from = scheduleInputDTO.getAvailableFrom();
        LocalTime to   = scheduleInputDTO.getAvailableTo();
        if (from == null || to == null) {
            throw new IllegalArgumentException("availableFrom và availableTo không được null.");
        }
        if (from.isAfter(to) || from.equals(to)) {
            throw new IllegalArgumentException("availableFrom phải trước availableTo và không được bằng nhau.");
        }
        schedule.setAvailableTo(scheduleInputDTO.getAvailableTo());
        schedule.setAvailableFrom(scheduleInputDTO.getAvailableFrom());
        schedule.setCarwash(carwashProfile);
        return schedule;
    }
}
