package com.wexa.graphed.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * TeamGraph service layer.
 *
 * Use case: an internal "who should I talk to" tool for an engineering org.
 * People, Skills and Projects form a graph; the interesting questions -
 * "who else knows what I know", "what's missing from this project's skill
 * set", "what should I learn next to bridge two skills" - are all path /
 * traversal questions that are natural in Cypher and awkward as repeated
 * self-joins in SQL.
 *
 * Every query below is parameterised via the official Neo4j driver - no
 * string concatenation of user input into Cypher.
 */
@Service
public class GraphService {

    @Autowired
    private Driver driver;

    // ---------------------------------------------------------------
    // 1. Seed
    // ---------------------------------------------------------------

    public Map<String, Object> seedDatabase() {
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");

            session.run("""
                CREATE (p1:Person {id:'1', name:'Sunil',  role:'Backend Engineer',  email:'sunil@wexa.ai'})
                CREATE (p2:Person {id:'2', name:'Alex',   role:'Fullstack Dev',     email:'alex@wexa.ai'})
                CREATE (p3:Person {id:'3', name:'Priya',  role:'Data Scientist',    email:'priya@wexa.ai'})
                CREATE (p4:Person {id:'4', name:'Meera',  role:'Frontend Engineer', email:'meera@wexa.ai'})
                CREATE (p5:Person {id:'5', name:'Ravi',   role:'DevOps Engineer',   email:'ravi@wexa.ai'})
                CREATE (p6:Person {id:'6', name:'Jordan', role:'ML Engineer',       email:'jordan@wexa.ai'})

                CREATE (s1:Skill {id:'101', name:'Java',        category:'Backend'})
                CREATE (s2:Skill {id:'102', name:'GraphDB',     category:'Database'})
                CREATE (s3:Skill {id:'103', name:'React',       category:'Frontend'})
                CREATE (s4:Skill {id:'104', name:'Python',      category:'Backend'})
                CREATE (s5:Skill {id:'105', name:'Kubernetes',  category:'Infra'})
                CREATE (s6:Skill {id:'106', name:'Docker',      category:'Infra'})
                CREATE (s7:Skill {id:'107', name:'Machine Learning', category:'Data'})
                CREATE (s8:Skill {id:'108', name:'SQL',         category:'Database'})
                CREATE (s9:Skill {id:'109', name:'TypeScript',  category:'Frontend'})

                CREATE (proj1:Project {id:'201', title:'Wexa AI Engine',      status:'Active',   description:'Core inference and orchestration engine'})
                CREATE (proj2:Project {id:'202', title:'Client Dashboard',    status:'Active',   description:'Customer-facing analytics dashboard'})
                CREATE (proj3:Project {id:'203', title:'Infra Migration',     status:'Planning',  description:'Migrate workloads onto Kubernetes'})

                CREATE (p1)-[:HAS_SKILL {proficiency:'Expert',      since:2019}]->(s1)
                CREATE (p1)-[:HAS_SKILL {proficiency:'Intermediate', since:2022}]->(s2)
                CREATE (p1)-[:HAS_SKILL {proficiency:'Intermediate', since:2021}]->(s8)

                CREATE (p2)-[:HAS_SKILL {proficiency:'Expert',      since:2020}]->(s3)
                CREATE (p2)-[:HAS_SKILL {proficiency:'Intermediate', since:2020}]->(s1)
                CREATE (p2)-[:HAS_SKILL {proficiency:'Beginner',    since:2024}]->(s9)

                CREATE (p3)-[:HAS_SKILL {proficiency:'Expert',      since:2018}]->(s4)
                CREATE (p3)-[:HAS_SKILL {proficiency:'Expert',      since:2019}]->(s7)
                CREATE (p3)-[:HAS_SKILL {proficiency:'Intermediate', since:2020}]->(s8)

                CREATE (p4)-[:HAS_SKILL {proficiency:'Expert',      since:2021}]->(s3)
                CREATE (p4)-[:HAS_SKILL {proficiency:'Intermediate', since:2023}]->(s9)

                CREATE (p5)-[:HAS_SKILL {proficiency:'Expert',      since:2017}]->(s5)
                CREATE (p5)-[:HAS_SKILL {proficiency:'Expert',      since:2017}]->(s6)
                CREATE (p5)-[:HAS_SKILL {proficiency:'Beginner',    since:2024}]->(s1)

                CREATE (p6)-[:HAS_SKILL {proficiency:'Expert',      since:2020}]->(s7)
                CREATE (p6)-[:HAS_SKILL {proficiency:'Intermediate', since:2021}]->(s4)

                CREATE (p1)-[:CONTRIBUTED_TO {role:'Lead',         since:2023}]->(proj1)
                CREATE (p2)-[:CONTRIBUTED_TO {role:'Frontend Dev', since:2023}]->(proj1)
                CREATE (p6)-[:CONTRIBUTED_TO {role:'ML Lead',      since:2023}]->(proj1)
                CREATE (p4)-[:CONTRIBUTED_TO {role:'Lead',         since:2024}]->(proj2)
                CREATE (p2)-[:CONTRIBUTED_TO {role:'Fullstack Dev',since:2024}]->(proj2)
                CREATE (p5)-[:CONTRIBUTED_TO {role:'Lead',         since:2024}]->(proj3)

                CREATE (proj1)-[:REQUIRES {priority:'High'}]->(s1)
                CREATE (proj1)-[:REQUIRES {priority:'High'}]->(s2)
                CREATE (proj1)-[:REQUIRES {priority:'Medium'}]->(s7)
                CREATE (proj2)-[:REQUIRES {priority:'High'}]->(s3)
                CREATE (proj2)-[:REQUIRES {priority:'Medium'}]->(s9)
                CREATE (proj2)-[:REQUIRES {priority:'Low'}]->(s8)
                CREATE (proj3)-[:REQUIRES {priority:'High'}]->(s5)
                CREATE (proj3)-[:REQUIRES {priority:'High'}]->(s6)

                CREATE (s1)-[:RELATED_TO {strength:0.8}]->(s2)
                CREATE (s1)-[:RELATED_TO {strength:0.5}]->(s8)
                CREATE (s4)-[:RELATED_TO {strength:0.9}]->(s7)
                CREATE (s4)-[:RELATED_TO {strength:0.6}]->(s8)
                CREATE (s3)-[:RELATED_TO {strength:0.9}]->(s9)
                CREATE (s5)-[:RELATED_TO {strength:0.85}]->(s6)
                CREATE (s2)-[:RELATED_TO {strength:0.4}]->(s8)

                CREATE (p1)-[:MENTORS]->(p5)
                CREATE (p3)-[:MENTORS]->(p6)
            """);

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("message", "Database seeded successfully");
            summary.put("people", 6);
            summary.put("skills", 9);
            summary.put("projects", 3);
            return summary;
        }
    }

    // ---------------------------------------------------------------
    // 2. Directory / profile lookups
    // ---------------------------------------------------------------

    public List<Map<String, Object>> getAllPeople() {
        try (Session session = driver.session()) {
            var result = session.run("""
                MATCH (p:Person)
                OPTIONAL MATCH (p)-[:HAS_SKILL]->(s:Skill)
                RETURN p.name AS name, p.role AS role, p.email AS email,
                       count(DISTINCT s) AS skillCount
                ORDER BY p.name
            """);
            List<Map<String, Object>> people = new ArrayList<>();
            for (Record record : result.list()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", record.get("name").asString());
                m.put("role", record.get("role").asString());
                m.put("email", record.get("email").asString());
                m.put("skillCount", record.get("skillCount").asInt());
                people.add(m);
            }
            return people;
        }
    }

    public Map<String, Object> getPersonProfile(String name) {
        try (Session session = driver.session()) {
            var personResult = session.run(
                    "MATCH (p:Person {name:$name}) RETURN p",
                    Values.parameters("name", name));
            if (!personResult.hasNext()) {
                throw new IllegalArgumentException("No person named '" + name + "' was found");
            }
            Node personNode = personResult.next().get("p").asNode();

            var skillsResult = session.run("""
                MATCH (p:Person {name:$name})-[r:HAS_SKILL]->(s:Skill)
                RETURN s.name AS skill, s.category AS category, r.proficiency AS proficiency
                ORDER BY s.name
            """, Values.parameters("name", name));

            List<Map<String, Object>> skills = new ArrayList<>();
            for (Record record : skillsResult.list()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("skill", record.get("skill").asString());
                m.put("category", record.get("category").asString());
                m.put("proficiency", record.get("proficiency").asString());
                skills.add(m);
            }

            var projectsResult = session.run("""
                MATCH (p:Person {name:$name})-[r:CONTRIBUTED_TO]->(proj:Project)
                RETURN proj.title AS project, proj.status AS status, r.role AS role
                ORDER BY proj.title
            """, Values.parameters("name", name));

            List<Map<String, Object>> projects = new ArrayList<>();
            for (Record record : projectsResult.list()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("project", record.get("project").asString());
                m.put("status", record.get("status").asString());
                m.put("role", record.get("role").asString());
                projects.add(m);
            }

            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("name", personNode.get("name").asString());
            profile.put("role", personNode.get("role").asString());
            profile.put("email", personNode.get("email").asString());
            profile.put("skills", skills);
            profile.put("projects", projects);
            return profile;
        }
    }

    public List<Map<String, Object>> getAllSkills() {
        try (Session session = driver.session()) {
            var result = session.run("MATCH (s:Skill) RETURN s.name AS name, s.category AS category ORDER BY s.category, s.name");
            List<Map<String, Object>> skills = new ArrayList<>();
            for (Record record : result.list()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", record.get("name").asString());
                m.put("category", record.get("category").asString());
                skills.add(m);
            }
            return skills;
        }
    }

    public List<Map<String, Object>> getAllProjects() {
        try (Session session = driver.session()) {
            var result = session.run("""
                MATCH (proj:Project)
                OPTIONAL MATCH (proj)<-[:CONTRIBUTED_TO]-(p:Person)
                RETURN proj.id AS id, proj.title AS title, proj.status AS status,
                       proj.description AS description, count(DISTINCT p) AS teamSize
                ORDER BY proj.title
            """);
            List<Map<String, Object>> projects = new ArrayList<>();
            for (Record record : result.list()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", record.get("id").asString());
                m.put("title", record.get("title").asString());
                m.put("status", record.get("status").asString());
                m.put("description", record.get("description").asString());
                m.put("teamSize", record.get("teamSize").asInt());
                projects.add(m);
            }
            return projects;
        }
    }

    // ---------------------------------------------------------------
    // 3. Multi-hop collaborator discovery (2 hops)
    //    "who shares skills with me, directly"
    // ---------------------------------------------------------------

    public List<Map<String, Object>> getCollaborators(String personName) {
        try (Session session = driver.session()) {
            var result = session.run("""
                MATCH (p:Person {name:$name})-[:HAS_SKILL]->(s:Skill)<-[:HAS_SKILL]-(colleague:Person)
                WHERE p <> colleague
                RETURN colleague.name AS collaborator, colleague.role AS role,
                       collect(DISTINCT s.name) AS sharedSkills,
                       count(DISTINCT s) AS overlap
                ORDER BY overlap DESC, collaborator
            """, Values.parameters("name", personName));

            List<Map<String, Object>> list = new ArrayList<>();
            for (Record record : result.list()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("collaborator", record.get("collaborator").asString());
                m.put("role", record.get("role").asString());
                m.put("sharedSkills", record.get("sharedSkills").asList(Object::toString));
                m.put("overlap", record.get("overlap").asInt());
                list.add(m);
            }
            return list;
        }
    }

    // ---------------------------------------------------------------
    // 4. Project-based multi-hop collaboration (3+ hops)
    //    "who worked with people who share skills required by projects I'm on"
    // ---------------------------------------------------------------

    public List<Map<String, Object>> getProjectNetwork(String personName) {
        try (Session session = driver.session()) {
            var result = session.run("""
                MATCH (p:Person {name:$name})-[:HAS_SKILL]->(s:Skill)<-[:REQUIRES]-(proj:Project)<-[:CONTRIBUTED_TO]-(colleague:Person)
                WHERE p <> colleague
                RETURN colleague.name AS collaborator, proj.title AS project, s.name AS sharedSkill
                ORDER BY project, collaborator
            """, Values.parameters("name", personName));

            List<Map<String, Object>> list = new ArrayList<>();
            for (Record record : result.list()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("collaborator", record.get("collaborator").asString());
                m.put("project", record.get("project").asString());
                m.put("sharedSkill", record.get("sharedSkill").asString());
                list.add(m);
            }
            return list;
        }
    }

    // ---------------------------------------------------------------
    // 5. Variable-length skill path (the query a relational DB finds
    //    awkward: an unbounded-depth traversal over a self-referencing
    //    Skill->Skill adjacency, done as recursive self-joins in SQL)
    // ---------------------------------------------------------------

    public Map<String, Object> getSkillLearningPath(String fromSkill, String toSkill) {
        try (Session session = driver.session()) {
            var result = session.run("""
                MATCH path = shortestPath((a:Skill {name:$from})-[:RELATED_TO*1..6]-(b:Skill {name:$to}))
                RETURN [n IN nodes(path) | n.name] AS steps, length(path) AS hops
            """, Values.parameters("from", fromSkill, "to", toSkill));

            Map<String, Object> response = new LinkedHashMap<>();
            if (result.hasNext()) {
                Record record = result.next();
                response.put("found", true);
                response.put("steps", record.get("steps").asList(Object::toString));
                response.put("hops", record.get("hops").asInt());
            } else {
                response.put("found", false);
                response.put("steps", List.of());
                response.put("hops", 0);
            }
            return response;
        }
    }

    // ---------------------------------------------------------------
    // 6. Skill gap analysis for a project
    // ---------------------------------------------------------------

    public List<Map<String, Object>> getSkillGap(String projectId) {
        try (Session session = driver.session()) {
            var result = session.run("""
                MATCH (proj:Project {id:$id})-[:REQUIRES]->(s:Skill)
                WHERE NOT EXISTS {
                    MATCH (proj)<-[:CONTRIBUTED_TO]-(:Person)-[:HAS_SKILL]->(s)
                }
                RETURN s.name AS missingSkill, s.category AS category
                ORDER BY s.name
            """, Values.parameters("id", projectId));

            List<Map<String, Object>> gaps = new ArrayList<>();
            for (Record record : result.list()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("missingSkill", record.get("missingSkill").asString());
                m.put("category", record.get("category").asString());
                gaps.add(m);
            }
            return gaps;
        }
    }

    // ---------------------------------------------------------------
    // 7. Team suggestions for a project - rank people not yet on the
    //    project by how many required skills they already have
    // ---------------------------------------------------------------

    public List<Map<String, Object>> getTeamSuggestions(String projectId) {
        try (Session session = driver.session()) {
            var result = session.run("""
                MATCH (proj:Project {id:$id})-[:REQUIRES]->(s:Skill)<-[:HAS_SKILL]-(candidate:Person)
                WHERE NOT EXISTS {
                    MATCH (candidate)-[:CONTRIBUTED_TO]->(proj)
                }
                RETURN candidate.name AS name, candidate.role AS role,
                       collect(DISTINCT s.name) AS matchingSkills,
                       count(DISTINCT s) AS matchCount
                ORDER BY matchCount DESC, name
            """, Values.parameters("id", projectId));

            List<Map<String, Object>> suggestions = new ArrayList<>();
            for (Record record : result.list()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", record.get("name").asString());
                m.put("role", record.get("role").asString());
                m.put("matchingSkills", record.get("matchingSkills").asList(Object::toString));
                m.put("matchCount", record.get("matchCount").asInt());
                suggestions.add(m);
            }
            return suggestions;
        }
    }
}
