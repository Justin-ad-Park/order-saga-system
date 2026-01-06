package com.example.couponservice.adapter.in.web;

import com.example.couponservice.adapter.in.web.dto.request.CompensateCouponRequest;
import com.example.couponservice.adapter.in.web.dto.request.ConfirmCouponRequest;
import com.example.couponservice.adapter.out.persistence.jpa.CouponJpaEntity;
import com.example.couponservice.adapter.out.persistence.jpa.CouponJpaRepository;
import com.example.couponservice.adapter.in.web.dto.request.ReserveCouponRequest;
import com.example.couponservice.domain.model.status.CouponStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.name=coupon_application"
)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CouponControllerIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    CouponJpaRepository couponJpaRepository;

//    @BeforeEach
//    void setUp() {
//        couponJpaRepository.deleteAll();
//    }

    @Test
    @Order(1)
    void reserveCoupon_shouldChangeStatusToReserved_andReturn200() {
        // given
        String couponNumber = "CPN-INT-AVAILABLE-001";

        //makeTestCoupon(couponNumber);

        String url = "http://localhost:" + port + "/api/v1/coupons/reserve";

        ReserveCouponRequest requestBody =
                new ReserveCouponRequest(couponNumber, "ORD-12345");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReserveCouponRequest> httpEntity =
                new HttpEntity<>(requestBody, headers);

        // when
        ResponseEntity<String> response =
                restTemplate.postForEntity(url, httpEntity, String.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        CouponJpaEntity updated =
                couponJpaRepository.findById(couponNumber).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CouponStatus.RESERVED);
    }

    @Test
    @Order(2)
    void reserveCoupon_shouldFailWhenAlreadyReserved() {
        // given
        String couponNumber = "CPN-INT-RESERVED-001";
        String url = "http://localhost:" + port + "/api/v1/coupons/reserve";

        ReserveCouponRequest requestBody =
                new ReserveCouponRequest(couponNumber, "ORD-12345");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReserveCouponRequest> httpEntity =
                new HttpEntity<>(requestBody, headers);

        // when
        ResponseEntity<String> response =
                restTemplate.postForEntity(url, httpEntity, String.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        CouponJpaEntity updated =
                couponJpaRepository.findById(couponNumber).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CouponStatus.RESERVED);
    }

    @Test
    @Order(3)
    void confirmCoupon_shouldChangeStatusToUsed_whenReserved() {
        String couponNumber = "CPN-INT-CONFIRM-001";
        makeTestCoupon(couponNumber);

        String reserveUrl = "http://localhost:" + port + "/api/v1/coupons/reserve";
        String confirmUrl = "http://localhost:" + port + "/api/v1/coupons/confirm";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ReserveCouponRequest reserveRequest =
                new ReserveCouponRequest(couponNumber, "ORD-12345");
        ResponseEntity<String> reserveResponse =
                restTemplate.postForEntity(
                        reserveUrl,
                        new HttpEntity<>(reserveRequest, headers),
                        String.class
                );
        assertThat(reserveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ConfirmCouponRequest confirmRequest =
                new ConfirmCouponRequest(couponNumber, "ORD-12345");
        ResponseEntity<String> confirmResponse =
                restTemplate.postForEntity(
                        confirmUrl,
                        new HttpEntity<>(confirmRequest, headers),
                        String.class
                );

        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        CouponJpaEntity updated =
                couponJpaRepository.findById(couponNumber).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CouponStatus.USED);
    }

    @Test
    @Order(4)
    void compensateCoupon_shouldChangeStatusToAvailable_whenReserved() {
        String couponNumber = "CPN-INT-COMPENSATE-001";
        makeTestCoupon(couponNumber);

        String reserveUrl = "http://localhost:" + port + "/api/v1/coupons/reserve";
        String compensateUrl = "http://localhost:" + port + "/api/v1/coupons/compensate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ReserveCouponRequest reserveRequest =
                new ReserveCouponRequest(couponNumber, "ORD-12346");
        ResponseEntity<String> reserveResponse =
                restTemplate.postForEntity(
                        reserveUrl,
                        new HttpEntity<>(reserveRequest, headers),
                        String.class
                );
        assertThat(reserveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        CompensateCouponRequest compensateRequest =
                new CompensateCouponRequest(couponNumber, "ORD-12346");
        ResponseEntity<String> compensateResponse =
                restTemplate.postForEntity(
                        compensateUrl,
                        new HttpEntity<>(compensateRequest, headers),
                        String.class
                );

        assertThat(compensateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        CouponJpaEntity updated =
                couponJpaRepository.findById(couponNumber).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CouponStatus.AVAILABLE);
    }

    private void makeTestCoupon(String couponNumber) {
        // H2에 사전 쿠폰 데이터 저장 (AVAILABLE 상태)
        CouponJpaEntity entity = new CouponJpaEntity(
                couponNumber,
                CouponStatus.AVAILABLE,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        );
        couponJpaRepository.save(entity);
    }
}
