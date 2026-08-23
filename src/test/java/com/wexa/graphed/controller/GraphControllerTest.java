package com.wexa.graphed.controller;

import com.wexa.graphed.service.GraphService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests. GraphService is mocked, so these verify request
 * routing, parameter binding and JSON shape - not the Cypher itself. The
 * Cypher queries are exercised against a real CognoDB instance manually
 * (see README "Tests" section).
 */
@WebMvcTest(GraphController.class)
class GraphControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GraphService graphService;

    @Test
    void getAllPeople_returnsList() throws Exception {
        Map<String, Object> person = new LinkedHashMap<>();
        person.put("name", "Sunil");
        person.put("role", "Backend Engineer");
        person.put("email", "sunil@wexa.ai");
        person.put("skillCount", 3);

        when(graphService.getAllPeople()).thenReturn(List.of(person));

        mockMvc.perform(get("/api/people"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sunil"))
                .andExpect(jsonPath("$[0].skillCount").value(3));
    }

    @Test
    void getAllPeople_emptyGraph_returnsEmptyList() throws Exception {
        when(graphService.getAllPeople()).thenReturn(List.of());

        mockMvc.perform(get("/api/people"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllSkills_returnsList() throws Exception {
        Map<String, Object> skill = new LinkedHashMap<>();
        skill.put("name", "Java");
        skill.put("category", "Backend");

        when(graphService.getAllSkills()).thenReturn(List.of(skill));

        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[0].category").value("Backend"));
    }

    @Test
    void getSkillLearningPath_found_returnsSteps() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("steps", List.of("Java", "GraphDB"));
        result.put("hops", 1);

        when(graphService.getSkillLearningPath("Java", "GraphDB")).thenReturn(result);

        mockMvc.perform(get("/api/skill-path").param("from", "Java").param("to", "GraphDB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.hops").value(1))
                .andExpect(jsonPath("$.steps[0]").value("Java"))
                .andExpect(jsonPath("$.steps[1]").value("GraphDB"));
    }

    @Test
    void getSkillLearningPath_notFound_returnsFoundFalse() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", false);
        result.put("steps", List.of());
        result.put("hops", 0);

        when(graphService.getSkillLearningPath(anyString(), anyString())).thenReturn(result);

        mockMvc.perform(get("/api/skill-path").param("from", "Java").param("to", "Kubernetes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.steps").isArray())
                .andExpect(jsonPath("$.steps").isEmpty());
    }

    @Test
    void getSkillGap_returnsMissingSkills() throws Exception {
        Map<String, Object> gap = new LinkedHashMap<>();
        gap.put("missingSkill", "Kubernetes");
        gap.put("category", "Infra");

        when(graphService.getSkillGap("203")).thenReturn(List.of(gap));

        mockMvc.perform(get("/api/projects/203/gap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].missingSkill").value("Kubernetes"));
    }

    @Test
    void getTeamSuggestions_returnsRankedCandidates() throws Exception {
        Map<String, Object> suggestion = new LinkedHashMap<>();
        suggestion.put("name", "Priya");
        suggestion.put("role", "Data Scientist");
        suggestion.put("matchingSkills", List.of("Python"));
        suggestion.put("matchCount", 1);

        when(graphService.getTeamSuggestions("201")).thenReturn(List.of(suggestion));

        mockMvc.perform(get("/api/projects/201/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Priya"))
                .andExpect(jsonPath("$[0].matchCount").value(1));
    }
}
