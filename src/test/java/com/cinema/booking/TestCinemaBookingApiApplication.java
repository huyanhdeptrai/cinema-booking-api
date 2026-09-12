package com.cinema.booking;

import org.springframework.boot.SpringApplication;

public class TestCinemaBookingApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(CinemaBookingApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
