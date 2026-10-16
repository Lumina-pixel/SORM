const USE_FAKE = false;                     
const API_BASE = "http://localhost:8080";    
const REFRESH_MS = 10000;                    
const HIGH_USAGE = 90;                      

let selectedId = null;

function pct(value) {
  if (value === null || value === undefined) return "-";
  return Number(value).toFixed(1) + "%";
}

function usageCell(value) {
  const cls = value > HIGH_USAGE ? "high" : "";
  return `<td class="${cls}">${pct(value)}</td>`;
}

function formatTime(iso) {
  return iso ? iso.replace("T", " ") : "-";
}

function showError(message) {
  const box = document.getElementById("errorBox");
  box.textContent = message;
  box.hidden = false;
}

function clearError() {
  document.getElementById("errorBox").hidden = true;
}

async function loadDevices() {
  const url = USE_FAKE ? "fake_devices.json" : `${API_BASE}/api/devices`;
  try {
    const response = await fetch(url);
    if (!response.ok) throw new Error("Server replied " + response.status);
    const devices = await response.json();
    clearError();
    renderDevices(devices);
    document.getElementById("updated").textContent =
      "Updated " + new Date().toLocaleTimeString();
  } catch (err) {
    showError("Cannot reach the server. Check that the Backend is running at " +
      API_BASE + ". Retrying every 10 seconds.");
    document.getElementById("updated").textContent = "Connection lost";
  }
}

function renderDevices(devices) {
  const body = document.getElementById("deviceBody");
  body.innerHTML = "";

  if (devices.length === 0) {
    body.innerHTML = `<tr><td colspan="7" class="empty">No computers registered yet. Start an Agent to see it here.</td></tr>`;
  }

  let online = 0;
  for (const d of devices) {
    if (d.status === "ONLINE") online++;
    const row = document.createElement("tr");
    if (d.deviceId === selectedId) row.className = "selected";
    row.innerHTML =
      `<td>${d.hostname}</td>` +
      `<td>${d.ipAddress || "-"}</td>` +
      `<td><span class="badge ${d.status.toLowerCase()}">${d.status}</span></td>` +
      usageCell(d.cpuUsage) + usageCell(d.ramUsage) + usageCell(d.diskUsage) +
      `<td>${formatTime(d.lastSeenAt)}</td>`;
    row.addEventListener("click", () => showHistory(d));
    body.appendChild(row);
  }

  document.getElementById("totalCount").textContent = devices.length;
  document.getElementById("onlineCount").textContent = online;
  document.getElementById("offlineCount").textContent = devices.length - online;

  if (selectedId !== null) {
    const current = devices.find(d => d.deviceId === selectedId);
    if (current) showHistory(current);
  }
}

async function showHistory(device) {
  selectedId = device.deviceId;
  document.getElementById("history").hidden = false;
  document.getElementById("historyTitle").textContent = "Recent readings: " + device.hostname;
  const body = document.getElementById("historyBody");

  let records;
  if (USE_FAKE) {
    records = [];
    for (let i = 0; i < 10; i++) {
      const t = new Date(Date.now() - i * 10000).toISOString().slice(0, 19);
      records.push({
        timestamp: t,
        cpuUsage: Math.min(100, Math.max(0, device.cpuUsage + (Math.random() * 10 - 5))),
        ramUsage: Math.min(100, Math.max(0, device.ramUsage + (Math.random() * 6 - 3))),
        diskUsage: device.diskUsage
      });
    }
  } else {
    try {
      const response = await fetch(`${API_BASE}/api/devices/${device.deviceId}/metrics?limit=10`);
      if (!response.ok) throw new Error("Server replied " + response.status);
      records = await response.json();
    } catch (err) {
      body.innerHTML = `<tr><td colspan="4" class="empty">Could not load history.</td></tr>`;
      return;
    }
  }

  body.innerHTML = records.map(r =>
    `<tr><td>${formatTime(r.timestamp)}</td>` +
    usageCell(r.cpuUsage) + usageCell(r.ramUsage) + usageCell(r.diskUsage) + `</tr>`
  ).join("");

  document.querySelectorAll("#deviceBody tr").forEach(tr => tr.classList.remove("selected"));
}

loadDevices();
setInterval(loadDevices, REFRESH_MS);