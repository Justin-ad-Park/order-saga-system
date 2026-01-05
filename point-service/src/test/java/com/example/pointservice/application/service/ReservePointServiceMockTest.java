package com.example.pointservice.application.service;

import com.example.pointservice.application.port.out.LoadPointPort;
import com.example.pointservice.application.port.out.SavePointPort;
import com.example.pointservice.domain.model.Point;
import com.example.pointservice.domain.model.status.PointStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ReservePointServiceMockTest {

    private LoadPointPort loadPointPort;
    private SavePointPort savePointPort;
    private ReservePointService reservePointService;

    @BeforeEach
    void setUp() {
        loadPointPort = mock(LoadPointPort.class);
        savePointPort = mock(SavePointPort.class);
        reservePointService = new ReservePointService(loadPointPort, savePointPort);
    }

    @Test
    void reserve_shouldChangeStatusToReserved_andSave() {
        // given
        String pointNumber = "PNT-UNIT-AVAILABLE-001";
        LocalDateTime now = LocalDateTime.now();
        Point availablePoint = new Point(pointNumber, PointStatus.AVAILABLE, now.minusDays(1), now.plusDays(1));

        when(loadPointPort.loadPoint(pointNumber)).thenReturn(Optional.of(availablePoint));

        // when
        reservePointService.reserve(pointNumber, "ORD-001");

        // then
        verify(loadPointPort, times(1)).loadPoint(pointNumber);
        verify(savePointPort, times(1)).save(argThat(saved ->
                saved.pointNumber().equals(pointNumber)
                        && saved.status() == PointStatus.RESERVED
        ));
    }

    @Test
    void confirm_shouldChangeStatusToUsed_andSave() {
        String pointNumber = "PNT-UNIT-RESERVED-001";
        LocalDateTime now = LocalDateTime.now();
        Point reserved = new Point(pointNumber, PointStatus.RESERVED, now.minusDays(1), now.plusDays(1));

        when(loadPointPort.loadPoint(pointNumber)).thenReturn(Optional.of(reserved));

        reservePointService.confirm(pointNumber, "ORD-004");

        verify(loadPointPort, times(1)).loadPoint(pointNumber);
        verify(savePointPort, times(1)).save(argThat(saved ->
                saved.pointNumber().equals(pointNumber)
                        && saved.status() == PointStatus.USED
        ));
    }

    @Test
    void confirm_shouldThrow_ifPointNotFound() {
        String pointNumber = "PNT-UNIT-NOTFOUND-002";
        when(loadPointPort.loadPoint(pointNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservePointService.confirm(pointNumber, "ORD-004"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("포인트를 찾을 수 없습니다");

        verify(savePointPort, never()).save(any());
    }

    @Test
    void confirm_shouldThrow_ifPointNotReserved() {
        String pointNumber = "PNT-UNIT-AVAILABLE-002";
        LocalDateTime now = LocalDateTime.now();
        Point available = new Point(pointNumber, PointStatus.AVAILABLE, now.minusDays(1), now.plusDays(1));
        when(loadPointPort.loadPoint(pointNumber)).thenReturn(Optional.of(available));

        assertThatThrownBy(() -> reservePointService.confirm(pointNumber, "ORD-004"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("확정 불가능한 포인트");

        verify(savePointPort, never()).save(any());
    }

    @Test
    void compensate_shouldChangeStatusToCompensated_andSave() {
        String pointNumber = "PNT-UNIT-RESERVED-002";
        LocalDateTime now = LocalDateTime.now();
        Point reserved = new Point(pointNumber, PointStatus.RESERVED, now.minusDays(1), now.plusDays(1));

        when(loadPointPort.loadPoint(pointNumber)).thenReturn(Optional.of(reserved));

        reservePointService.compensatePoint(pointNumber, "ORD-005");

        verify(loadPointPort, times(1)).loadPoint(pointNumber);
        verify(savePointPort, times(1)).save(argThat(saved ->
                saved.pointNumber().equals(pointNumber)
                        && saved.status() == PointStatus.COMPENSATED
        ));
    }

    @Test
    void compensate_shouldNoOp_ifPointNotFound() {
        String pointNumber = "PNT-UNIT-NOTFOUND-003";
        when(loadPointPort.loadPoint(pointNumber)).thenReturn(Optional.empty());

        reservePointService.compensatePoint(pointNumber, "ORD-005");

        verify(savePointPort, never()).save(any());
    }

    @Test
    void compensate_shouldNoOp_ifPointNotReserved() {
        String pointNumber = "PNT-UNIT-AVAILABLE-003";
        LocalDateTime now = LocalDateTime.now();
        Point available = new Point(pointNumber, PointStatus.AVAILABLE, now.minusDays(1), now.plusDays(1));
        when(loadPointPort.loadPoint(pointNumber)).thenReturn(Optional.of(available));

        reservePointService.compensatePoint(pointNumber, "ORD-005");

        verify(savePointPort, never()).save(any());
    }

    @Test
    void reserve_shouldThrow_ifPointNotFound() {
        String pointNumber = "PNT-UNIT-NOTFOUND-001";
        when(loadPointPort.loadPoint(pointNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservePointService.reserve(pointNumber, "ORD-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("포인트를 찾을 수 없습니다");

        verify(savePointPort, never()).save(any());
    }

    @Test
    void reserve_shouldThrow_ifPointNotAvailable() {
        String pointNumber = "PNT-UNIT-RESERVED-001";
        LocalDateTime now = LocalDateTime.now();
        Point reserved = new Point(pointNumber, PointStatus.RESERVED, now.minusDays(1), now.plusDays(1));
        when(loadPointPort.loadPoint(pointNumber)).thenReturn(Optional.of(reserved));

        assertThatThrownBy(() -> reservePointService.reserve(pointNumber, "ORD-002"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("예약 불가능한 포인트");

        verify(savePointPort, never()).save(any());
    }

    @Test
    void reserve_shouldThrow_ifPointAlreadyUsed() {
        String pointNumber = "PNT-UNIT-USED-001";
        LocalDateTime now = LocalDateTime.now();
        Point used = new Point(pointNumber, PointStatus.USED, now.minusDays(1), now.plusDays(1));
        when(loadPointPort.loadPoint(pointNumber)).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> reservePointService.reserve(pointNumber, "ORD-003"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("예약 불가능한 포인트");

        verify(savePointPort, never()).save(any());
    }
}
