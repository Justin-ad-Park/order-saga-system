package com.example.pointservice.adapter.in.web;

import com.example.pointservice.adapter.out.persistence.jpa.PointJpaEntity;
import com.example.pointservice.adapter.out.persistence.jpa.PointJpaRepository;
import com.example.pointservice.adapter.in.web.dto.request.ReservePointRequest;
import com.example.pointservice.domain.model.status.PointStatus;
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
        properties = "spring.config.name=point_application"
)
@ActiveProfiles("test")
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
    void reservePoint_shouldChangeStatusToReserved_andReturn200() {
        // given
        String pointNumber = "P-001";

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
