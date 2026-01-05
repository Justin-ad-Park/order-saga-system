package com.example.pointservice.adapter.in.web;

import com.example.common.api.ApiResponse;
import com.example.pointservice.adapter.in.web.dto.request.CompensatePointRequest;
import com.example.pointservice.adapter.in.web.dto.request.ConfirmPointRequest;
import com.example.pointservice.adapter.in.web.dto.request.ReservePointRequest;
import com.example.pointservice.adapter.in.web.dto.response.CompensatePointResponse;
import com.example.pointservice.adapter.in.web.dto.response.ConfirmPointResponse;
import com.example.pointservice.adapter.in.web.dto.response.ReservePointResponse;
import com.example.pointservice.application.port.in.CompensatePointUseCase;
import com.example.pointservice.application.port.in.ConfirmPointUseCase;
import com.example.pointservice.application.port.in.ReservePointUseCase;
import com.example.pointservice.domain.model.status.PointStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final ReservePointUseCase reservePointUseCase;
    private final ConfirmPointUseCase confirmPointUseCase;
    private final CompensatePointUseCase compensatePointUseCase;

    @PostMapping("/reserve")
    public ApiResponse<ReservePointResponse> reservePoint(@RequestBody ReservePointRequest request) {
        reservePointUseCase.reserve(request.pointNumber(), request.orderId());

        return ApiResponse.success(buildReserveResponse(request.pointNumber(), PointStatus.RESERVED));
    }

    @PostMapping("/confirm")
    public ApiResponse<ConfirmPointResponse> confirmPoint(@RequestBody ConfirmPointRequest request) {
        confirmPointUseCase.confirm(request.pointNumber(), request.orderId());
        return ApiResponse.success(buildConfirmResponse(request.pointNumber(), PointStatus.USED));
    }

    @PostMapping("/compensate")
    public ApiResponse<CompensatePointResponse> compensatePoint(@RequestBody CompensatePointRequest request) {
        compensatePointUseCase.compensatePoint(request.pointNumber(), request.orderId());
        return ApiResponse.success(buildCompensateResponse(request.pointNumber(), PointStatus.COMPENSATED));
    }

    private ReservePointResponse buildReserveResponse(String pointNumber, PointStatus status) {
        return new ReservePointResponse(
                pointNumber,
                status.name()
        );
    }

    private ConfirmPointResponse buildConfirmResponse(String pointNumber, PointStatus status) {
        return new ConfirmPointResponse(
                pointNumber,
                status.name()
        );
    }

    private CompensatePointResponse buildCompensateResponse(String pointNumber, PointStatus status) {
        return new CompensatePointResponse(
                pointNumber,
                status.name()
        );
    }
}
