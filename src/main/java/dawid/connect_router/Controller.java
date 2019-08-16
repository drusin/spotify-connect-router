package dawid.connect_router;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.body.MultipartBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
public class Controller {

	@Value("${client_id}")
	private String clientId;
	@Value("${client_secret}")
	private String clientSecret;
	@Value("${redirect_url}")
	private String redirectUrl;

	@Autowired
	TokenRepository tokenRepository;
	@Autowired
	AliasDeviceRepository aliasRepository;

	private boolean retrying = false;

	@RequestMapping(path = "/login", method = RequestMethod.GET)
	public RedirectView login(@RequestParam("uuid") UUID uuid) {
		if (!ConnectRouterApplication.getUuid().equals(uuid)) {
			return null;
		}
		String url = "https://accounts.spotify.com/authorize?client_id="
				+ clientId
				+ "&response_type=code"
				+ "&scope=user-read-playback-state user-modify-playback-state"
				+ "&redirect_uri="
				+ redirectUrl;
		return new RedirectView(url);
	}

	@RequestMapping(path = "/redirect", method = RequestMethod.GET)
	public RedirectView redirect(@RequestParam("code") String code) throws Exception {
		MultipartBody multipartBody = Unirest.post("https://accounts.spotify.com/api/token")
				.field("client_id", clientId)
				.field("client_secret", clientSecret)
				.field("grant_type", "authorization_code")
				.field("code", code)
				.field("redirect_uri", redirectUrl);
		HttpResponse<String> response = multipartBody
				.asString();
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			Token token = objectMapper.readValue(response.getBody(), Token.class);
			if (tokenRepository.findById(0).isPresent()) {
				tokenRepository.deleteById(0);
			}
			tokenRepository.save(token);
			System.out.println("Logged in succesfully!");
			retrying = false;
			return new RedirectView("/redirected.html?Logged in succesfully");
		} catch (IOException e) {
			System.err.println("Login failed");
			return new RedirectView("/redirected.html?Something went wrong, try again");
		}
	}

	private void reauthenticate() throws Exception {
		System.out.println("Trying to refresh the token!");
		String refreshToken = tokenRepository.findById(0).get().getRefreshToken();
		MultipartBody multipartBody = Unirest.post("https://accounts.spotify.com/api/token")
				.field("client_id", clientId)
				.field("client_secret", clientSecret)
				.field("grant_type", "refresh_token")
				.field("refresh_token", refreshToken);
		HttpResponse<String> response = multipartBody
				.asString();
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			Token token = objectMapper.readValue(response.getBody(), Token.class);
			if (tokenRepository.findById(0).isPresent()) {
				tokenRepository.deleteById(0);
			}
			token.setRefreshToken(refreshToken);
			tokenRepository.save(token);
			System.out.println("Refreshed token successfully!");
		} catch (IOException e) {
			System.err.println("Could not refresh token");
			System.err.println(response.getStatus() + " " + response.getStatusText() + " " + response.getBody());
		}
	}

	@RequestMapping(path = "/devices", method = RequestMethod.GET)
	public String devices(@RequestParam("uuid") UUID uuid) throws Exception {
		if (!ConnectRouterApplication.getUuid().equals(uuid)) {
			return null;
		}
		HttpResponse<JsonNode> response = Unirest.get("https://api.spotify.com/v1/me/player/devices")
				.header("Authorization", "Bearer " + tokenRepository.findById(0).get().getAccessToken())
				.asJson();
		if (response.getStatus() != 200) {
			if (response.getStatus() == 401 && !retrying) {
				retrying = true;
				reauthenticate();
				return devices(uuid);
			}
			System.err.println("Error trying to get devices: " + response.getStatus() + " " + response.getStatusText() + " " + response.getBody());
			return "Cannot get devices " + response.getStatus() + " " + response.getStatusText() + " " + response.getBody();
		}
		retrying = false;
		return response.getBody().toString();
	}

	@RequestMapping(path = "/transferPlayback", method = RequestMethod.PUT)
	public String transferPlayback(@RequestParam("uuid") UUID uuid, @RequestBody Alias alias) throws Exception {
		if (!uuid.equals(ConnectRouterApplication.getUuid())) {
			return "wrong UUID!";
		}
		HttpResponse<String> response = Unirest.put("https://api.spotify.com/v1/me/player")
				.header("Authorization", "Bearer " + tokenRepository.findById(0).get().getAccessToken())
				.body("{\"device_ids\": [\"" + aliasRepository.findById(alias.alias.trim()).get().deviceId + "\"]}")
				.asString();
		if (response.getStatus() != 204) {
			if (response.getStatus() == 401 && !retrying) {
				retrying = true;
				reauthenticate();
				return transferPlayback(uuid, alias);
			}
			System.err.println("Error trying to transfer playback: " + response.getStatus() + " " + response.getStatusText() + " " + response.getBody());
			return "Cannot transfer playback " + response.getStatus() + " " + response.getStatusText() + " " + response.getBody();
		}
		retrying = false;
		return Integer.toString(response.getStatus());
	}

	@RequestMapping(path = "/alias", method = RequestMethod.PUT)
	public void setAlias(@RequestParam("uuid") UUID uuid, @RequestBody AliasDeviceMapping alias) {
		if (!uuid.equals(ConnectRouterApplication.getUuid())) {
			return;
		}
		aliasRepository.save(alias);
	}

	@RequestMapping(path = "/alias", method = RequestMethod.GET)
	public List<AliasDeviceMapping> getAliases() {
		return StreamSupport.stream(aliasRepository.findAll().spliterator(), false).collect(Collectors.toList());
	}

	@RequestMapping(path = "pause", method = RequestMethod.PUT)
	public void pause(@RequestParam("uuid") UUID uuid) throws Exception {
		if (!uuid.equals(ConnectRouterApplication.getUuid())) {
			return;
		}
		HttpResponse<String> response = Unirest.put("https://api.spotify.com/v1/me/player/pause")
				.header("Authorization", "Bearer " + tokenRepository.findById(0).get().getAccessToken())
				.asString();
		if (response.getStatus() != 204) {
			if (response.getStatus() == 401 && !retrying) {
				retrying = true;
				reauthenticate();
				pause(uuid);
			}
			System.err.println("Error trying to pause playback: " + response.getStatus() + " " + response.getStatusText() + " " + response.getBody());
		}
		retrying = false;
	}

	@RequestMapping(path = "play", method = RequestMethod.PUT)
	public void play(@RequestParam("uuid") UUID uuid) throws Exception {
		if (!uuid.equals(ConnectRouterApplication.getUuid())) {
			return;
		}
		HttpResponse<String> response = Unirest.put("https://api.spotify.com/v1/me/player/play")
				.header("Authorization", "Bearer " + tokenRepository.findById(0).get().getAccessToken())
				.asString();
		if (response.getStatus() != 204) {
			if (response.getStatus() == 401 && !retrying) {
				retrying = true;
				reauthenticate();
				play(uuid);
			}
			System.err.println("Error trying to play: " + response.getStatus() + " " + response.getStatusText() + " " + response.getBody());
		}
		retrying = false;
	}

	@RequestMapping(path = "next", method = RequestMethod.PUT)
	public void next(@RequestParam("uuid") UUID uuid) throws Exception {
		if (!uuid.equals(ConnectRouterApplication.getUuid())) {
			return;
		}
		HttpResponse<String> response = Unirest.post("https://api.spotify.com/v1/me/player/next")
				.header("Authorization", "Bearer " + tokenRepository.findById(0).get().getAccessToken())
				.asString();
		if (response.getStatus() != 204) {
			if (response.getStatus() == 401 && !retrying) {
				retrying = true;
				reauthenticate();
				next(uuid);
			}
			System.err.println("Error trying to next: " + response.getStatus() + " " + response.getStatusText() + " " + response.getBody());
		}
		retrying = false;
	}

	@RequestMapping(path = "previous", method = RequestMethod.PUT)
	public void previous(@RequestParam("uuid") UUID uuid) throws Exception {
		if (!uuid.equals(ConnectRouterApplication.getUuid())) {
			return;
		}
		HttpResponse<String> response = Unirest.post("https://api.spotify.com/v1/me/player/previous")
				.header("Authorization", "Bearer " + tokenRepository.findById(0).get().getAccessToken())
				.asString();
		if (response.getStatus() != 204) {
			if (response.getStatus() == 401 && !retrying) {
				retrying = true;
				reauthenticate();
				previous(uuid);
			}
			System.err.println("Error trying to previous: " + response.getStatus() + " " + response.getStatusText() + " " + response.getBody());
		}
		retrying = false;
	}

	@RequestMapping(path = "volume", method = RequestMethod.PUT)
	public int setVolume(@RequestParam("uuid") UUID uuid, @RequestParam("volume") int volume) throws Exception {
		if (!uuid.equals(ConnectRouterApplication.getUuid())) {
			return 0;
		}
		HttpResponse<String> response = Unirest.put("https://api.spotify.com/v1/me/player/volume?volume_percent=" + volume)
				.header("Authorization", "Bearer " + tokenRepository.findById(0).get().getAccessToken())
				.asString();
		if (response.getStatus() != 204) {
			if (response.getStatus() == 401 && !retrying) {
				retrying = true;
				reauthenticate();
				return setVolume(uuid, volume);
			}
			System.err.println("Error trying to set volume: " + response.getStatus() + " " + response.getStatusText() + " " + response.getBody());
			return response.getStatus();
		}
		retrying = false;
		return response.getStatus();
	}
}