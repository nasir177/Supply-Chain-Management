package com.sorascm.backend.fulfillment.controller;

import com.sorascm.backend.common.dto.ApiResponse;
import com.sorascm.backend.fulfillment.dto.TaskExecutionDto;
import com.sorascm.backend.fulfillment.service.TaskExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fulfillment/tasks")
public class TaskExecutionController {

    private final TaskExecutionService taskExecutionService;

    public TaskExecutionController(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    @PostMapping("/orders/{orderId}/generate-picks")
    public ResponseEntity<ApiResponse<List<TaskExecutionDto.PickTaskResponse>>> generatePickTasks(
            @PathVariable UUID orderId,
            @RequestParam(defaultValue = "1") Long locationId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(taskExecutionService.generatePickTasks(orderId, locationId), "Pick tasks generated"));
    }

    @GetMapping("/orders/{orderId}/picks")
    public ResponseEntity<ApiResponse<List<TaskExecutionDto.PickTaskResponse>>> getOrderPicks(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.ok(taskExecutionService.getPickTasksForOrder(orderId)));
    }

    @PostMapping("/picks/{pickTaskId}/confirm")
    public ResponseEntity<ApiResponse<TaskExecutionDto.PickTaskResponse>> confirmPick(
            @PathVariable UUID pickTaskId,
            @Valid @RequestBody TaskExecutionDto.ConfirmPickRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(taskExecutionService.confirmPick(pickTaskId, req), "Pick task confirmed"));
    }

    @PostMapping("/orders/{orderId}/pack")
    public ResponseEntity<ApiResponse<TaskExecutionDto.PackTaskResponse>> createPack(
            @PathVariable UUID orderId,
            @Valid @RequestBody TaskExecutionDto.CreatePackRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(taskExecutionService.createPackTask(orderId, req), "Package sealed and packed"));
    }

    @GetMapping("/orders/{orderId}/packs")
    public ResponseEntity<ApiResponse<List<TaskExecutionDto.PackTaskResponse>>> getOrderPacks(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.ok(taskExecutionService.getPackTasksForOrder(orderId)));
    }
}