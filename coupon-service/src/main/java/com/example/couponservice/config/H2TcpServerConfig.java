//package com.example.couponservice.config;
//
//import org.h2.tools.Server;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//
//import java.sql.SQLException;
//
//@Configuration
//@Profile({"local", "test"}) // ★ 운영에서는 절대 로딩되지 않게
//public class H2TcpServerConfig {
//
//    @Bean(initMethod = "start", destroyMethod = "stop")
//    public Server h2TcpServer() throws SQLException {
//        return Server.createTcpServer(
//                "-tcp",
//                "-tcpAllowOthers",
//                "-tcpPort", "9092"
//        );
//    }
//}