package com.example.pointservice.adapter.in.web;

import com.example.common.api.ApiResponse;
import com.example.pointservice.adapter.in.web.dto.request.ReservePointRequest;
import com.example.pointservice.adapter.in.web.dto.response.ReservePointResponse;
import com.example.pointservice.application.port.in.ReservePointUseCase;
import com.example.pointservice.domain.model.status.PointStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final ReservePointUseCase reservePointUseCase;

    @PostMapping("/reserve")
    public ApiResponse<ReservePointResponse> reservePoint(@RequestBody ReservePointRequest request) {
        reservePointUseCase.reserve(request.pointNumber(), request.orderId());

        // 지금은 단순히 RESERVED 라고 응답만 내려줌
        ReservePointResponse response = new ReservePointResponse(
                request.pointNumber(),
                PointStatus.RESERVED.name()
        );
        return ApiResponse.success(response);
    }
}
