const table = document.getElementsByTagName("table")[0];
const template = document.getElementById("template-row");
const uuidInput = document.getElementById("uuid");

function uuid() {
	return "?uuid=" + uuidInput.value;
}

function saveAlias(id, alias) {
	$.ajax({
		data: JSON.stringify({
			alias: alias,
			deviceId: id
		}),
		type: "PUT",
		url: "/alias" + uuid(),
		dataType: "json",
		headers: {"Content-Type": "application/json"},
		success: () => displayDevices()
	});
}

function play(alias) {
	$.ajax({
		data: JSON.stringify({
			alias: alias
		}),
		type: "PUT",
		url: "/transferPlayback" + uuid(),
		dataType: "json",
		headers: {"Content-Type": "application/json"}
	});
}

function displayDevices() {
	const oldRows = document.getElementsByClassName("row");
	for (let i = 0; i < oldRows.length; i++) {
		table.removeChild(oldRows[i]);
	}
	$.get("/devices" + uuid()).done(response => {
		JSON.parse(response).devices.forEach(device => {
			const row = template.cloneNode(true);
			row.setAttribute("data-id", device.id);
			row.setAttribute("id", "");
			row.setAttribute("class", "row");
			row.getElementsByClassName("name")[0].innerHTML = device.name;
			const alias = row.getElementsByTagName("input")[0];
			alias.setAttribute("data-name", device.id);
			row.getElementsByClassName("save")[0].onclick = () => saveAlias(device.id, alias.value);
			row.getElementsByClassName("play")[0].onclick = () => play(alias.value);
			row.style.display = "";
			table.appendChild(row);
		});
		displayAliases();
	});
}

uuidInput.oninput = displayDevices;

document.getElementById("reload").onclick = displayDevices;

displayDevices();

function displayAliases() {
	$.get("/alias").done(response => {
		response.forEach(alias => {
			const inputs = document.querySelectorAll("[data-name='" + alias.deviceId + "']");
			if (inputs.length > 0) {
				inputs[0].value = alias.alias;
			}
		});
	});
}