package com.example.couponservice.adapter.in.web;

import com.example.couponservice.adapter.out.persistence.jpa.CouponJpaEntity;
import com.example.couponservice.adapter.out.persistence.jpa.CouponJpaRepository;
import com.example.couponservice.adapter.in.web.dto.request.ReserveCouponRequest;
import com.example.couponservice.domain.model.status.CouponStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.name=coupon_application"
)
@ActiveProfiles("test")
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
    void reserveCoupon_shouldChangeStatusToReserved_andReturn200() {
        // given
        String couponNumber = "C-001";

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
