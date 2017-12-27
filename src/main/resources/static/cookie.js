const uuidInput = document.getElementById("uuid");
const rememberUuid = document.getElementById("remember-uuid");

uuidInput.oninput = doRemember;
rememberUuid.onchange = doRemember;

function doRemember() {
	if (rememberUuid.checked) {
		document.cookie = "uuid=" + uuidInput.value + "; expires=" + new Date(new Date().getFullYear + 5).toUTCString() + ", path=/";
	}
	else {
		document.cookie = "uuid=; expires=Thu, 01 Jan 1970 00:00:00 UTC, path=/";
	}
}

function uuid() {
	return "?uuid=" + uuidInput.value;
}

if (document.cookie.indexOf("uuid=") != -1) {
	uuidInput.value = document.cookie.split("=")[1];
	rememberUuid.checked = true;
}