// TeamGraph seed data — mirrors GraphService.seedDatabase()
// Run this manually against CognoDB via the console's query editor or
// `cypher-shell` if you want to seed without going through the app.
// (The app also seeds itself via POST /api/seed — this file exists so the
// data model is inspectable without reading Java.)

MATCH (n) DETACH DELETE n;

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

CREATE (proj1:Project {id:'201', title:'Wexa AI Engine',   status:'Active',  description:'Core inference and orchestration engine'})
CREATE (proj2:Project {id:'202', title:'Client Dashboard', status:'Active',  description:'Customer-facing analytics dashboard'})
CREATE (proj3:Project {id:'203', title:'Infra Migration',  status:'Planning', description:'Migrate workloads onto Kubernetes'})

CREATE (p1)-[:HAS_SKILL {proficiency:'Expert',       since:2019}]->(s1)
CREATE (p1)-[:HAS_SKILL {proficiency:'Intermediate', since:2022}]->(s2)
CREATE (p1)-[:HAS_SKILL {proficiency:'Intermediate', since:2021}]->(s8)
CREATE (p2)-[:HAS_SKILL {proficiency:'Expert',       since:2020}]->(s3)
CREATE (p2)-[:HAS_SKILL {proficiency:'Intermediate', since:2020}]->(s1)
CREATE (p2)-[:HAS_SKILL {proficiency:'Beginner',     since:2024}]->(s9)
CREATE (p3)-[:HAS_SKILL {proficiency:'Expert',       since:2018}]->(s4)
CREATE (p3)-[:HAS_SKILL {proficiency:'Expert',       since:2019}]->(s7)
CREATE (p3)-[:HAS_SKILL {proficiency:'Intermediate', since:2020}]->(s8)
CREATE (p4)-[:HAS_SKILL {proficiency:'Expert',       since:2021}]->(s3)
CREATE (p4)-[:HAS_SKILL {proficiency:'Intermediate', since:2023}]->(s9)
CREATE (p5)-[:HAS_SKILL {proficiency:'Expert',       since:2017}]->(s5)
CREATE (p5)-[:HAS_SKILL {proficiency:'Expert',       since:2017}]->(s6)
CREATE (p5)-[:HAS_SKILL {proficiency:'Beginner',     since:2024}]->(s1)
CREATE (p6)-[:HAS_SKILL {proficiency:'Expert',       since:2020}]->(s7)
CREATE (p6)-[:HAS_SKILL {proficiency:'Intermediate', since:2021}]->(s4)

CREATE (p1)-[:CONTRIBUTED_TO {role:'Lead',          since:2023}]->(proj1)
CREATE (p2)-[:CONTRIBUTED_TO {role:'Frontend Dev',  since:2023}]->(proj1)
CREATE (p6)-[:CONTRIBUTED_TO {role:'ML Lead',       since:2023}]->(proj1)
CREATE (p4)-[:CONTRIBUTED_TO {role:'Lead',          since:2024}]->(proj2)
CREATE (p2)-[:CONTRIBUTED_TO {role:'Fullstack Dev', since:2024}]->(proj2)
CREATE (p5)-[:CONTRIBUTED_TO {role:'Lead',          since:2024}]->(proj3)

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
CREATE (p3)-[:MENTORS]->(p6);
