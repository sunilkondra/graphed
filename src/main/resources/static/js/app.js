const API = "/api";

// ---------------- helpers ----------------

async function apiGet(path) {
  const res = await fetch(API + path);
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || "Request failed");
  return body;
}

async function apiPost(path) {
  const res = await fetch(API + path, { method: "POST" });
  const body = await res.json();
  if (!res.ok) throw new Error(body.message || "Request failed");
  return body;
}

function setStatus(msg, kind) {
  const el = document.getElementById("statusLine");
  el.textContent = msg;
  el.className = "status-line" + (kind ? " " + kind : "");
}

function el(tag, className, html) {
  const e = document.createElement(tag);
  if (className) e.className = className;
  if (html !== undefined) e.innerHTML = html;
  return e;
}

function errorBox(container, message) {
  container.innerHTML = "";
  container.appendChild(el("div", "error-box", `⚠ ${message}`));
}

// ---------------- navigation ----------------

document.querySelectorAll(".nav-item").forEach((btn) => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".nav-item").forEach((b) => b.classList.remove("is-active"));
    document.querySelectorAll(".view").forEach((v) => v.classList.remove("is-active"));
    btn.classList.add("is-active");
    document.getElementById("view-" + btn.dataset.view).classList.add("is-active");
    if (btn.dataset.view === "directory") loadDirectory();
    if (btn.dataset.view === "projects") loadProjects();
    if (btn.dataset.view === "path") loadSkillOptions();
  });
});

// ---------------- seed ----------------

document.getElementById("seedBtn").addEventListener("click", async () => {
  setStatus("Seeding…");
  try {
    const result = await apiPost("/seed");
    setStatus(`Seeded: ${result.people} people, ${result.skills} skills, ${result.projects} projects`, "ok");
    await loadPeopleSelect();
  } catch (e) {
    setStatus("Seed failed — " + e.message, "err");
  }
});

// ---------------- explorer ----------------

let network = null;

async function loadPeopleSelect() {
  try {
    const people = await apiGet("/people");
    const select = document.getElementById("personSelect");
    select.innerHTML = "";
    if (people.length === 0) {
      select.appendChild(el("option", null, "No people yet — seed the DB"));
      return;
    }
    people.forEach((p) => {
      const opt = el("option", null, `${p.name} — ${p.role}`);
      opt.value = p.name;
      select.appendChild(opt);
    });
  } catch (e) {
    setStatus("Could not reach CognoDB — " + e.message, "err");
  }
}

document.getElementById("loadCollabBtn").addEventListener("click", traceConnections);

async function traceConnections() {
  const name = document.getElementById("personSelect").value;
  if (!name) return;

  const collabList = document.getElementById("collabList");
  const networkList = document.getElementById("networkList");
  collabList.innerHTML = "<p class='empty-note'>Loading…</p>";
  networkList.innerHTML = "<p class='empty-note'>Loading…</p>";

  try {
    const [collaborators, projectNetwork, profile] = await Promise.all([
      apiGet(`/collaborators?name=${encodeURIComponent(name)}`),
      apiGet(`/project-network?name=${encodeURIComponent(name)}`),
      apiGet(`/people/${encodeURIComponent(name)}`),
    ]);

    renderCollaborators(collabList, collaborators);
    renderProjectNetwork(networkList, projectNetwork);
    renderGraph(name, profile, collaborators, projectNetwork);
  } catch (e) {
    errorBox(collabList, e.message);
    networkList.innerHTML = "";
  }
}

function renderCollaborators(container, list) {
  container.innerHTML = "";
  if (list.length === 0) {
    container.appendChild(el("p", "empty-note", "No one else shares a skill with this person yet."));
    return;
  }
  list.forEach((c) => {
    const card = el("div", "row-card");
    card.innerHTML = `<div class="row-title"><span>${c.collaborator}</span><span>${c.overlap} shared</span></div>
      <div class="row-sub">${c.role}</div>`;
    const chipRow = el("div");
    c.sharedSkills.forEach((s) => chipRow.appendChild(el("span", "chip", s)));
    card.appendChild(chipRow);
    container.appendChild(card);
  });
}

function renderProjectNetwork(container, list) {
  container.innerHTML = "";
  if (list.length === 0) {
    container.appendChild(el("p", "empty-note", "No project-based connections found."));
    return;
  }
  list.forEach((c) => {
    const card = el("div", "row-card");
    card.innerHTML = `<div class="row-title"><span>${c.collaborator}</span></div>
      <div class="row-sub">via <strong>${c.project}</strong> — shared skill: ${c.sharedSkill}</div>`;
    container.appendChild(card);
  });
}

function renderGraph(centerName, profile, collaborators, projectNetwork) {
  const canvas = document.getElementById("graphCanvas");
  document.getElementById("canvasEmpty")?.remove();

  const nodes = new Map();
  const edges = [];

  const addNode = (id, label, group) => {
    if (!nodes.has(id)) nodes.set(id, { id, label, group });
  };

  addNode("p:" + centerName, centerName, "person");

  (profile.skills || []).forEach((s) => {
    addNode("s:" + s.skill, s.skill, "skill");
    edges.push({ from: "p:" + centerName, to: "s:" + s.skill, label: s.proficiency, color: { color: "#4fd1c5", opacity: 0.5 } });
  });

  collaborators.forEach((c) => {
    addNode("p:" + c.collaborator, c.collaborator, "person");
    c.sharedSkills.forEach((s) => {
      addNode("s:" + s, s, "skill");
      edges.push({ from: "p:" + c.collaborator, to: "s:" + s, color: { color: "#f2b84b", opacity: 0.4 } });
    });
  });

  projectNetwork.forEach((c) => {
    addNode("proj:" + c.project, c.project, "project");
    addNode("p:" + c.collaborator, c.collaborator, "person");
    edges.push({ from: "p:" + c.collaborator, to: "proj:" + c.project, color: { color: "#e8607b", opacity: 0.5 } });
  });

  const groupColors = {
    person: { background: "rgba(242,184,75,0.18)", border: "#f2b84b" },
    skill: { background: "rgba(79,209,197,0.18)", border: "#4fd1c5" },
    project: { background: "rgba(232,96,123,0.18)", border: "#e8607b" },
  };

  const visNodes = Array.from(nodes.values()).map((n) => ({
    id: n.id,
    label: n.label,
    color: groupColors[n.group],
    font: { color: "#e8ecf1", size: 12, face: "Inter" },
    shape: n.id === "p:" + centerName ? "star" : "dot",
    size: n.id === "p:" + centerName ? 22 : 14,
  }));

  const data = { nodes: new vis.DataSet(visNodes), edges: new vis.DataSet(edges) };
  const options = {
    physics: { stabilization: true, barnesHut: { gravitationalConstant: -4000, springLength: 110 } },
    interaction: { hover: true },
    edges: { smooth: { type: "continuous" }, width: 1.5 },
  };

  if (network) network.destroy();
  network = new vis.Network(canvas, data, options);
}

// ---------------- directory ----------------

async function loadDirectory() {
  const grid = document.getElementById("directoryGrid");
  grid.innerHTML = "<p class='empty-note'>Loading…</p>";
  try {
    const people = await apiGet("/people");
    grid.innerHTML = "";
    if (people.length === 0) {
      grid.appendChild(el("p", "empty-note", "No one in the graph yet — seed the database from the sidebar."));
      return;
    }
    people.forEach((p) => {
      const card = el("div", "person-card");
      const initials = p.name.split(" ").map((w) => w[0]).join("").slice(0, 2).toUpperCase();
      card.innerHTML = `<div class="avatar">${initials}</div>
        <div class="name">${p.name}</div>
        <div class="role">${p.role}</div>
        <div class="skills">${p.skillCount} skill${p.skillCount === 1 ? "" : "s"}</div>`;
      grid.appendChild(card);
    });
  } catch (e) {
    errorBox(grid, e.message);
  }
}

// ---------------- projects ----------------

async function loadProjects() {
  const list = document.getElementById("projectList");
  list.innerHTML = "<p class='empty-note'>Loading…</p>";
  try {
    const projects = await apiGet("/projects");
    list.innerHTML = "";
    if (projects.length === 0) {
      list.appendChild(el("p", "empty-note", "No projects yet — seed the database from the sidebar."));
      return;
    }
    projects.forEach((proj) => {
      const card = el("div", "project-card");
      card.innerHTML = `<div class="title"><span>${proj.title}</span><span class="status-tag">${proj.status}</span></div>
        <div class="desc">${proj.description || ""}</div>
        <div class="team">${proj.teamSize} on team</div>`;
      card.addEventListener("click", () => {
        document.querySelectorAll(".project-card").forEach((c) => c.classList.remove("is-active"));
        card.classList.add("is-active");
        loadProjectDetail(proj.id);
      });
      list.appendChild(card);
    });
  } catch (e) {
    errorBox(list, e.message);
  }
}

async function loadProjectDetail(id) {
  const gapList = document.getElementById("gapList");
  const suggestList = document.getElementById("suggestList");
  gapList.innerHTML = "<p class='empty-note'>Loading…</p>";
  suggestList.innerHTML = "<p class='empty-note'>Loading…</p>";

  try {
    const [gaps, suggestions] = await Promise.all([
      apiGet(`/projects/${encodeURIComponent(id)}/gap`),
      apiGet(`/projects/${encodeURIComponent(id)}/suggestions`),
    ]);

    gapList.innerHTML = "";
    if (gaps.length === 0) {
      gapList.appendChild(el("p", "empty-note", "No gaps — every required skill is covered by the team."));
    } else {
      gaps.forEach((g) => {
        const card = el("div", "row-card");
        card.innerHTML = `<div class="row-title"><span>${g.missingSkill}</span></div><div class="row-sub">${g.category}</div>`;
        gapList.appendChild(card);
      });
    }

    suggestList.innerHTML = "";
    if (suggestions.length === 0) {
      suggestList.appendChild(el("p", "empty-note", "No one outside the team matches the required skills."));
    } else {
      suggestions.forEach((s) => {
        const card = el("div", "row-card");
        card.innerHTML = `<div class="row-title"><span>${s.name}</span><span>${s.matchCount} match${s.matchCount === 1 ? "" : "es"}</span></div>
          <div class="row-sub">${s.role}</div>`;
        const chipRow = el("div");
        s.matchingSkills.forEach((sk) => chipRow.appendChild(el("span", "chip", sk)));
        card.appendChild(chipRow);
        suggestList.appendChild(card);
      });
    }
  } catch (e) {
    errorBox(gapList, e.message);
    suggestList.innerHTML = "";
  }
}

// ---------------- skill path ----------------

async function loadSkillOptions() {
  const fromSel = document.getElementById("fromSkillSelect");
  const toSel = document.getElementById("toSkillSelect");
  try {
    const skills = await apiGet("/skills");
    [fromSel, toSel].forEach((sel) => (sel.innerHTML = ""));
    skills.forEach((s) => {
      const o1 = el("option", null, s.name); o1.value = s.name;
      const o2 = el("option", null, s.name); o2.value = s.name;
      fromSel.appendChild(o1);
      toSel.appendChild(o2);
    });
    if (skills.length > 1) toSel.selectedIndex = 1;
  } catch (e) {
    setStatus("Could not load skills — " + e.message, "err");
  }
}

document.getElementById("findPathBtn").addEventListener("click", async () => {
  const from = document.getElementById("fromSkillSelect").value;
  const to = document.getElementById("toSkillSelect").value;
  const result = document.getElementById("pathResult");
  result.innerHTML = "<p class='empty-note'>Searching…</p>";

  try {
    const data = await apiGet(`/skill-path?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
    result.innerHTML = "";
    if (!data.found) {
      result.appendChild(el("p", "empty-note", `No related-skill path exists between "${from}" and "${to}" within 6 hops.`));
      return;
    }
    const chain = el("div", "path-chain");
    data.steps.forEach((step, i) => {
      chain.appendChild(el("span", "path-node", step));
      if (i < data.steps.length - 1) chain.appendChild(el("span", "path-sep", "⟶"));
    });
    result.appendChild(chain);
    result.appendChild(el("p", "path-meta", `${data.hops} hop${data.hops === 1 ? "" : "s"} via RELATED_TO`));
  } catch (e) {
    errorBox(result, e.message);
  }
});

// ---------------- boot ----------------

(async function init() {
  await loadPeopleSelect();
})();
