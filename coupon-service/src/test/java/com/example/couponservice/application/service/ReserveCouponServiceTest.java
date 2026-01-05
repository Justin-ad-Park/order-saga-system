package com.example.couponservice.application.service;

import com.example.couponservice.application.port.out.LoadCouponPort;
import com.example.couponservice.application.port.out.SaveCouponPort;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.status.CouponStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ReserveCouponServiceTest {

    private LoadCouponPort loadCouponPort;
    private SaveCouponPort saveCouponPort;
    private ReserveCouponService reserveCouponService;

    @BeforeEach
    void setUp() {
        loadCouponPort = mock(LoadCouponPort.class);
        saveCouponPort = mock(SaveCouponPort.class);
        reserveCouponService = new ReserveCouponService(loadCouponPort, saveCouponPort);
    }

    @Test
    void reserve_shouldChangeStatusToReserved_andSave() {
        // given
        String couponNumber = "CPN-UNIT-AVAILABLE-001";
        LocalDateTime now = LocalDateTime.now();
        Coupon availableCoupon = new Coupon(couponNumber, CouponStatus.AVAILABLE, now.minusDays(1), now.plusDays(1));

        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.of(availableCoupon));

        // when
        reserveCouponService.reserve(couponNumber, "ORD-001");

        // then
        verify(loadCouponPort, times(1)).loadCoupon(couponNumber);
        verify(saveCouponPort, times(1)).save(argThat(saved ->
                saved.couponNumber().equals(couponNumber)
                        && saved.status() == CouponStatus.RESERVED
        ));
    }

    @Test
    void confirm_shouldChangeStatusToUsed_andSave() {
        String couponNumber = "CPN-UNIT-RESERVED-001";
        LocalDateTime now = LocalDateTime.now();
        Coupon reserved = new Coupon(couponNumber, CouponStatus.RESERVED, now.minusDays(1), now.plusDays(1));

        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.of(reserved));

        reserveCouponService.confirm(couponNumber, "ORD-004");

        verify(loadCouponPort, times(1)).loadCoupon(couponNumber);
        verify(saveCouponPort, times(1)).save(argThat(saved ->
                saved.couponNumber().equals(couponNumber)
                        && saved.status() == CouponStatus.USED
        ));
    }

    @Test
    void confirm_shouldThrow_ifCouponNotFound() {
        String couponNumber = "CPN-UNIT-NOTFOUND-002";
        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reserveCouponService.confirm(couponNumber, "ORD-004"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("쿠폰을 찾을 수 없습니다");

        verify(saveCouponPort, never()).save(any());
    }

    @Test
    void confirm_shouldThrow_ifCouponNotReserved() {
        String couponNumber = "CPN-UNIT-AVAILABLE-002";
        LocalDateTime now = LocalDateTime.now();
        Coupon available = new Coupon(couponNumber, CouponStatus.AVAILABLE, now.minusDays(1), now.plusDays(1));
        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.of(available));

        assertThatThrownBy(() -> reserveCouponService.confirm(couponNumber, "ORD-004"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("확정 불가능한 쿠폰");

        verify(saveCouponPort, never()).save(any());
    }

    @Test
    void reserve_shouldThrow_ifCouponNotFound() {
        String couponNumber = "CPN-UNIT-NOTFOUND-001";
        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reserveCouponService.reserve(couponNumber, "ORD-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("쿠폰을 찾을 수 없습니다");

        verify(saveCouponPort, never()).save(any());
    }

    @Test
    void reserve_shouldThrow_ifCouponNotAvailable() {
        String couponNumber = "CPN-UNIT-RESERVED-001";
        LocalDateTime now = LocalDateTime.now();
        Coupon reserved = new Coupon(couponNumber, CouponStatus.RESERVED, now.minusDays(1), now.plusDays(1));
        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.of(reserved));

        assertThatThrownBy(() -> reserveCouponService.reserve(couponNumber, "ORD-002"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("예약 불가능한 쿠폰");

        verify(saveCouponPort, never()).save(any());
    }

    @Test
    void reserve_shouldThrow_ifCouponAlreadyUsed() {
        String couponNumber = "CPN-UNIT-USED-001";
        LocalDateTime now = LocalDateTime.now();
        Coupon used = new Coupon(couponNumber, CouponStatus.USED, now.minusDays(1), now.plusDays(1));
        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> reserveCouponService.reserve(couponNumber, "ORD-003"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("예약 불가능한 쿠폰");

        verify(saveCouponPort, never()).save(any());
    }
}
