package com.lab.recruitment.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Order(2)
@ConditionalOnProperty(value = "app.modules.growth-center.enabled", havingValue = "true")
public class GrowthCenterDataInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    public GrowthCenterDataInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        populator.addScript(new ClassPathResource("sql/growth_center_schema.sql"));
        populator.addScript(new ClassPathResource("sql/growth_center_seed_tracks.sql"));
        populator.addScript(new ClassPathResource("sql/growth_center_seed_assessment.sql"));
        DatabasePopulatorUtils.execute(populator, dataSource);
    }
}
