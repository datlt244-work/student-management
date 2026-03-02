package com.newwave.student_management.domains.facility.controller;

import com.newwave.student_management.common.dto.ApiResponse;
import com.newwave.student_management.domains.facility.dto.request.AdminCreateRoomRequest;
import com.newwave.student_management.domains.facility.dto.request.AdminUpdateRoomRequest;
import com.newwave.student_management.domains.facility.dto.response.AdminRoomListResponse;
import com.newwave.student_management.domains.facility.dto.response.AdminRoomResponse;
import com.newwave.student_management.domains.facility.service.AdminRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomController {

    private final AdminRoomService adminRoomService;

    @GetMapping
    public ApiResponse<AdminRoomListResponse> getRooms(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String roomType,
            Pageable pageable) {

        return ApiResponse.success(adminRoomService.getRooms(search, roomType, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminRoomResponse> getRoomDetail(@PathVariable Integer id) {
        return ApiResponse.success(adminRoomService.getRoomDetail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminRoomResponse> createRoom(
            @Valid @RequestBody AdminCreateRoomRequest request) {
        return ApiResponse.success(adminRoomService.createRoom(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminRoomResponse> updateRoom(
            @PathVariable Integer id,
            @Valid @RequestBody AdminUpdateRoomRequest request) {
        return ApiResponse.success(adminRoomService.updateRoom(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRoom(@PathVariable Integer id) {
        adminRoomService.deleteRoom(id);
        return ApiResponse.success(null);
    }
}
