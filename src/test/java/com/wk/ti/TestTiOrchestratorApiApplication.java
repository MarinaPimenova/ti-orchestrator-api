package com.wk.ti;

import org.springframework.boot.SpringApplication;

public class TestTiOrchestratorApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(TiOrchestratorApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
