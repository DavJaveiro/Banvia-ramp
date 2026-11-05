package com.banvia;

import org.springframework.boot.SpringApplication;

public class TestBanviaApplication {

    public static void main(String[] args) {
        SpringApplication.from(BanviaApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
