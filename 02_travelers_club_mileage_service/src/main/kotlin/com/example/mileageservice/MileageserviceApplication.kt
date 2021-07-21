package com.example.mileageservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class MileageserviceApplication

fun main(args: Array<String>) {
	runApplication<MileageserviceApplication>(*args)
}
