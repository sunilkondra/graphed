package com.wexa.graphed.controller;

import com.wexa.graphed.service.GraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GraphController {

    @Autowired
    private GraphService graphService;

    // --- Setup -------------------------------------------------------

    @PostMapping("/seed")
    public Map<String, Object> seed() {
        return graphService.seedDatabase();
    }

    // --- Directory -----------------------------------------------------

    @GetMapping("/people")
    public List<Map<String, Object>> getAllPeople() {
        return graphService.getAllPeople();
    }

    @GetMapping("/people/{name}")
    public Map<String, Object> getPersonProfile(@PathVariable String name) {
        return graphService.getPersonProfile(name);
    }

    @GetMapping("/skills")
    public List<Map<String, Object>> getAllSkills() {
        return graphService.getAllSkills();
    }

    @GetMapping("/projects")
    public List<Map<String, Object>> getAllProjects() {
        return graphService.getAllProjects();
    }

    // --- Graph queries -------------------------------------------------

    // 2-hop: people who directly share a skill with you
    @GetMapping("/collaborators")
    public List<Map<String, Object>> getCollaborators(@RequestParam String name) {
        return graphService.getCollaborators(name);
    }

    // 4-hop: people connected to you via a shared project's skill requirements
    @GetMapping("/project-network")
    public List<Map<String, Object>> getProjectNetwork(@RequestParam String name) {
        return graphService.getProjectNetwork(name);
    }

    // variable-length shortest path between two skills over RELATED_TO
    @GetMapping("/skill-path")
    public Map<String, Object> getSkillLearningPath(@RequestParam String from, @RequestParam String to) {
        return graphService.getSkillLearningPath(from, to);
    }

    // skills a project needs that nobody on the team currently has
    @GetMapping("/projects/{id}/gap")
    public List<Map<String, Object>> getSkillGap(@PathVariable String id) {
        return graphService.getSkillGap(id);
    }

    // people not yet on a project, ranked by how many required skills they have
    @GetMapping("/projects/{id}/suggestions")
    public List<Map<String, Object>> getTeamSuggestions(@PathVariable String id) {
        return graphService.getTeamSuggestions(id);
    }
}
