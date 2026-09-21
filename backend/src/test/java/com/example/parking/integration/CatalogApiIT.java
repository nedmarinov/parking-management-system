package com.example.parking.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDatabaseConfig.class)
@Transactional
class CatalogApiIT {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void listsUsers() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":1,"name":"Alex Johnson","balance":"20.00"},
                         {"id":2,"name":"Maria Smith","balance":"10.00"}]""", JsonCompareMode.STRICT));
    }

    @Test
    void getsUser() throws Exception {
        mvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":2,"name":"Maria Smith","balance":"10.00"}""", JsonCompareMode.STRICT));
    }

    @Test
    void listsOnlyTheUsersVehicles() throws Exception {
        mvc.perform(get("/api/users/1/vehicles"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":1,"plateNumber":"CA1234AB"},{"id":2,"plateNumber":"CB5678CD"}]""", JsonCompareMode.STRICT));
    }

    @Test
    void existingUserWithoutVehiclesGetsEmptyList() throws Exception {
        Long userId = jdbc.queryForObject("INSERT INTO users (name) VALUES ('No Cars') RETURNING id", Long.class);

        mvc.perform(get("/api/users/" + userId + "/vehicles"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    void listsCitiesByName() throws Exception {
        mvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":2,"name":"Plovdiv"},{"id":1,"name":"Sofia"}]""", JsonCompareMode.STRICT));
    }

    @Test
    void listsOnlyActiveZonesOfCity() throws Exception {
        mvc.perform(get("/api/cities/2/zones"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":3,"name":"Blue Zone","cityId":2,"pricePerHour":"1.50","active":true},
                         {"id":4,"name":"Green Zone","cityId":2,"pricePerHour":"1.00","active":true}]""", JsonCompareMode.STRICT));
    }

    @Test
    void existingCityWithoutActiveZonesGetsEmptyList() throws Exception {
        Long cityId = jdbc.queryForObject("INSERT INTO cities (name) VALUES ('Varna') RETURNING id", Long.class);

        mvc.perform(get("/api/cities/" + cityId + "/zones"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/users/999", "/api/users/999/vehicles"})
    void missingUserIsNotFound(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"code":"USER_NOT_FOUND","message":"User not found"}""", JsonCompareMode.STRICT));
    }

    @Test
    void missingCityIsNotFound() throws Exception {
        mvc.perform(get("/api/cities/999/zones"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"code":"CITY_NOT_FOUND","message":"City not found"}""", JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/users/abc", "/api/users/0", "/api/users/-1", "/api/users/99999999999999999999",
            "/api/users/0/vehicles", "/api/cities/abc/zones", "/api/cities/0/zones"})
    void invalidIdentifierIsBadRequest(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"code":"INVALID_REQUEST","message":"Invalid request"}""", JsonCompareMode.STRICT));
    }
}
