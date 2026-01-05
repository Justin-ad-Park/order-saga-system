package com.example.pointservice.adapter.in.web;

import com.example.pointservice.adapter.in.web.dto.request.CompensatePointRequest;
import com.example.pointservice.adapter.in.web.dto.request.ConfirmPointRequest;
import com.example.pointservice.adapter.out.persistence.jpa.PointJpaEntity;
import com.example.pointservice.adapter.out.persistence.jpa.PointJpaRepository;
import com.example.pointservice.adapter.in.web.dto.request.ReservePointRequest;
import com.example.pointservice.domain.model.status.PointStatus;
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
        properties = "spring.config.name=point_application"
)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PointControllerIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    PointJpaRepository pointJpaRepository;

//    @BeforeEach
//    void setUp() {
//        pointJpaRepository.deleteAll();
//    }

    @Test
    @Order(1)
    void reservePoint_shouldChangeStatusToReserved_andReturn200() {
        // given
        String pointNumber = "PNT-INT-AVAILABLE-001";

        //makeTestPoint(pointNumber);

        String url = "http://localhost:" + port + "/api/v1/points/reserve";

        ReservePointRequest requestBody =
                new ReservePointRequest(pointNumber, "ORD-12345");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReservePointRequest> httpEntity =
                new HttpEntity<>(requestBody, headers);

        // when
        ResponseEntity<String> response =
                restTemplate.postForEntity(url, httpEntity, String.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        PointJpaEntity updated =
                pointJpaRepository.findById(pointNumber).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PointStatus.RESERVED);
    }

    @Test
    @Order(2)
    void reservePoint_shouldFailWhenAlreadyReserved() {
        // given
        String pointNumber = "PNT-INT-RESERVED-001";
        String url = "http://localhost:" + port + "/api/v1/points/reserve";

        ReservePointRequest requestBody =
                new ReservePointRequest(pointNumber, "ORD-12345");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReservePointRequest> httpEntity =
                new HttpEntity<>(requestBody, headers);

        // when
        ResponseEntity<String> response =
                restTemplate.postForEntity(url, httpEntity, String.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        PointJpaEntity updated =
                pointJpaRepository.findById(pointNumber).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PointStatus.RESERVED);
    }

    @Test
    @Order(3)
    void confirmPoint_shouldChangeStatusToUsed_whenReserved() {
        String pointNumber = "PNT-INT-CONFIRM-001";
        makeTestPoint(pointNumber);

        String reserveUrl = "http://localhost:" + port + "/api/v1/points/reserve";
        String confirmUrl = "http://localhost:" + port + "/api/v1/points/confirm";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ReservePointRequest reserveRequest =
                new ReservePointRequest(pointNumber, "ORD-12345");
        ResponseEntity<String> reserveResponse =
                restTemplate.postForEntity(
                        reserveUrl,
                        new HttpEntity<>(reserveRequest, headers),
                        String.class
                );
        assertThat(reserveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ConfirmPointRequest confirmRequest =
                new ConfirmPointRequest(pointNumber, "ORD-12345");
        ResponseEntity<String> confirmResponse =
                restTemplate.postForEntity(
                        confirmUrl,
                        new HttpEntity<>(confirmRequest, headers),
                        String.class
                );

        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        PointJpaEntity updated =
                pointJpaRepository.findById(pointNumber).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PointStatus.USED);
    }

    @Test
    @Order(4)
    void compensatePoint_shouldChangeStatusToCompensated_whenReserved() {
        String pointNumber = "PNT-INT-COMPENSATE-001";
        makeTestPoint(pointNumber);

        String reserveUrl = "http://localhost:" + port + "/api/v1/points/reserve";
        String compensateUrl = "http://localhost:" + port + "/api/v1/points/compensate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ReservePointRequest reserveRequest =
                new ReservePointRequest(pointNumber, "ORD-12346");
        ResponseEntity<String> reserveResponse =
                restTemplate.postForEntity(
                        reserveUrl,
                        new HttpEntity<>(reserveRequest, headers),
                        String.class
                );
        assertThat(reserveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        CompensatePointRequest compensateRequest =
                new CompensatePointRequest(pointNumber, "ORD-12346");
        ResponseEntity<String> compensateResponse =
                restTemplate.postForEntity(
                        compensateUrl,
                        new HttpEntity<>(compensateRequest, headers),
                        String.class
                );

        assertThat(compensateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        PointJpaEntity updated =
                pointJpaRepository.findById(pointNumber).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PointStatus.COMPENSATED);
    }

    private void makeTestPoint(String pointNumber) {
        // H2에 사전 쿠폰 데이터 저장 (AVAILABLE 상태)
        PointJpaEntity entity = new PointJpaEntity(
                pointNumber,
                PointStatus.AVAILABLE,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        );
        pointJpaRepository.save(entity);
    }
}
