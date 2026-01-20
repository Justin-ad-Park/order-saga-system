package com.example.couponservice.application.service;

import com.example.couponservice.application.port.in.CompensateCouponUseCase;
import com.example.couponservice.application.port.in.ConfirmCouponUseCase;
import com.example.couponservice.application.port.in.ReserveCouponUseCase;
import com.example.couponservice.application.port.out.LoadCouponPort;
import com.example.couponservice.application.port.out.LoadCouponReservationPort;
import com.example.couponservice.application.port.out.SaveCouponPort;
import com.example.couponservice.application.port.out.SaveCouponReservationPort;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.CouponReservation;
import com.example.couponservice.domain.model.status.CouponStatus;
import com.example.couponservice.domain.model.status.ReservationStatus;
import jakarta.transaction.Transactional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ReserveCouponService implements ReserveCouponUseCase, ConfirmCouponUseCase, CompensateCouponUseCase {

    private final LoadCouponPort loadCouponPort;
    private final SaveCouponPort saveCouponPort;
    private final LoadCouponReservationPort loadCouponReservationPort;
    private final SaveCouponReservationPort saveCouponReservationPort;

    @Override
    public void reserve(String couponNumber, String orderId) {
        if (isReservationCancelled(orderId)) {
            return;
        }
        verifyReservationNotAlreadyReserved(orderId);
        updateStatus(couponNumber, CouponStatus.RESERVED, this::validateReservable);
        saveCouponReservationPort.saveReservation(new CouponReservation(
                orderId,
                couponNumber,
                ReservationStatus.RESERVED
        ));
    }

    @Override
    public void confirm(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));
        if (coupon.status() == CouponStatus.USED) {
            return;
        }
        validateConfirmable(coupon);

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                CouponStatus.USED,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(updated);
    }

    @Override
    public void compensateCoupon(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElse(null);
        if (coupon == null) {
            saveReservationCancelled(orderId, couponNumber);
            return;
        }
        if (coupon.status() == CouponStatus.USED) {
            throw new IllegalStateException("보상 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }

        saveReservationCancelled(orderId, couponNumber);
        if (coupon.status() != CouponStatus.RESERVED) { // RESERVED 일 때만 보상 처리
            return;
        }

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                CouponStatus.AVAILABLE,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(updated);
    }

    private boolean isReservationCancelled(String orderId) {
        return loadCouponReservationPort.loadReservation(orderId)
                .map(reservation -> reservation.status() == ReservationStatus.CANCELLED)
                .orElse(false);
    }

    private void verifyReservationNotAlreadyReserved(String orderId) {
        loadCouponReservationPort.loadReservation(orderId)
                .filter(reservation -> reservation.status() == ReservationStatus.RESERVED)
                .ifPresent(reservation -> {
                    throw new IllegalStateException("이미 예약된 주문입니다: " + reservation.orderId());
                });
    }

    private void saveReservationCancelled(String orderId, String couponNumber) {
        saveCouponReservationPort.saveReservation(new CouponReservation(
                orderId,
                couponNumber,
                ReservationStatus.CANCELLED
        ));
    }

    private void updateStatus(
            String couponNumber,
            CouponStatus targetStatus,
            Consumer<Coupon> validator
    ) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));

        validator.accept(coupon);

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                targetStatus,
                coupon.issuedAt(),
                coupon.expiredAt()
        );

        saveCouponPort.save(updated);
    }

    private void validateReservable(Coupon coupon) {
        if (!coupon.isAvailable()) {
            throw new IllegalStateException("예약 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }
    }

    private void validateConfirmable(Coupon coupon) {
        if (coupon.status() != CouponStatus.RESERVED) {
            throw new IllegalStateException("확정 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }
    }

}
