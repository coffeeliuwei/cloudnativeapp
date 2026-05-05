package com.coffee.yun.coffeeapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class CoffeeAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoffeeAppApplication.class, args);
	}

}
