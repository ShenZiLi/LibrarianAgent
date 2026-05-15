package com.librarian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LibrarianAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibrarianAgentApplication.class, args);
    }
}
