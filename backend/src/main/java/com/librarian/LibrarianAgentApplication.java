package com.librarian;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LibrarianAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibrarianAgentApplication.class, args);
    }
}
