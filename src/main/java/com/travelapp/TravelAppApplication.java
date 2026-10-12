package com.travelapp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @MapperScan must point only to packages that actually contain Mapper interfaces,
 * not the whole base application package "com.travelapp" —
 * otherwise MyBatis will treat ordinary business interfaces like AuthService/UserService as Mappers too,
 * generating a fake proxy bean that overrides our own implementation classes (AuthServiceImpl, etc.).
 * The app starts fine but any call fails with "Invalid bound statement (not found)".
 * Remember to add the new mapper package here whenever the customize/itinerary module gets its own mapper package.
 */
@SpringBootApplication
@MapperScan({"com.travelapp.user.mapper", "com.travelapp.content.mapper", "com.travelapp.customize.mapper", "com.travelapp.itinerary.mapper"})
public class TravelAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelAppApplication.class, args);
    }

}
