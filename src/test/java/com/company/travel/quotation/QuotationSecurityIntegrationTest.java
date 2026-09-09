package com.company.travel.quotation;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.entity.UserGroupMapping;
import com.company.travel.auth.repository.UserGroupMappingRepository;
import com.company.travel.auth.repository.UserGroupRepository;
import com.company.travel.auth.repository.UserRepository;
import com.company.travel.auth.dto.LoginResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuotationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserGroupMappingRepository userGroupMappingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void jwtSecurityChainEnforcesQuotationOwnership() throws Exception {
        String password = "Str0ng!Passw0rd123";
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User userA = createMappedUser("quotation-owner-a-" + suffix,
                "quotation-owner-a-" + suffix + "@company.com", password);
        User userB = createMappedUser("quotation-owner-b-" + suffix,
                "quotation-owner-b-" + suffix + "@company.com", password);

        String tokenA = login(userA.getUsername(), password);
        String tokenB = login(userB.getUsername(), password);

        String createResponse = mockMvc.perform(post("/api/v1/quotations")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "travellerName", "Ravi Kumar",
                        "travellerDateOfBirth", "1990-01-01",
                        "passportNumber", "P1234567",
                        "originCountry", "LK",
                        "destinationCountry", "FR",
                        "destinationCity", "Paris",
                        "travelStartDate", LocalDate.now().plusDays(1).toString(),
                        "travelEndDate", LocalDate.now().plusDays(10).toString(),
                        "coverType", "TRAVEL",
                        "sumInsured", 10000))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long quotationId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/v1/quotations/" + quotationId)
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/quotations/" + quotationId)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        JsonNode userBList = objectMapper.readTree(mockMvc.perform(get("/api/v1/quotations")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertEquals(0, userBList.size());
    }

    private User createMappedUser(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(username);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        UserGroup group = userGroupRepository.findByGroupCode("UNDERWRITING_USER").orElseThrow();
        UserGroupMapping mapping = new UserGroupMapping();
        mapping.setUserId(user.getId());
        mapping.setUserGroupId(group.getId());
        mapping.setEffectiveFrom(LocalDate.now());
        userGroupMappingRepository.save(mapping);
        return user;
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", username,
                        "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(body, LoginResponse.class).getAccessToken();
    }
}