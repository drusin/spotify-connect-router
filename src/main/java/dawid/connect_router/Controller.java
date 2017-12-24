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
	public RedirectView login() {
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
		HttpResponse<String> stringHttpResponse = multipartBody
				.asString();
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			Token token = objectMapper.readValue(stringHttpResponse.getBody(), Token.class);
			if (tokenRepository.findOne(0) != null) {
				tokenRepository.delete(0);
			}
			tokenRepository.save(token);
			return new RedirectView("/redirected.html?Logged in succesfully");
		} catch (IOException e) {
			return new RedirectView("/redirected.html?Something went wrong, try again");
		}
	}

	public RedirectView reauthenticate() throws Exception {
		MultipartBody multipartBody = Unirest.post("https://accounts.spotify.com/api/token")
				.field("client_id", clientId)
				.field("client_secret", clientSecret)
				.field("grant_type", "refresh_token")
				.field("refresh_token", tokenRepository.findOne(0).getRefreshToken());
		HttpResponse<String> stringHttpResponse = multipartBody
				.asString();
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			Token token = objectMapper.readValue(stringHttpResponse.getBody(), Token.class);
			if (tokenRepository.findOne(0) != null) {
				tokenRepository.delete(0);
			}
			tokenRepository.save(token);
			return new RedirectView("/redirected.html?Logged in succesfully");
		} catch (IOException e) {
			return new RedirectView("/redirected.html?Something went wrong, try again");
		}
	}

	@RequestMapping(path = "/devices", method = RequestMethod.GET)
	public String devices(@RequestParam("uuid") String uuid) throws Exception {
		if (!ConnectRouterApplication.getUuid().toString().equals(uuid)) {
			return null;
		}
		HttpResponse<JsonNode> request = Unirest.get("https://api.spotify.com/v1/me/player/devices")
				.header("Authorization", "Bearer " + tokenRepository.findOne(0).getAccessToken())
				.asJson();
		if (request.getStatus() != 200) {
			if (!retrying) {
				retrying = true;
				reauthenticate();
				devices(uuid);
			}
			return "Cannot authenticate, try logging in again!";
		}
		retrying = false;
		return request.getBody().toString();
	}

	@RequestMapping(path = "/transferPlayback", method = RequestMethod.PUT)
	public String transferPlayback(@RequestParam("uuid") UUID uuid, @RequestBody Alias alias) throws Exception {
		if (!uuid.equals(ConnectRouterApplication.getUuid())) {
			return "wrong UUID!";
		}
		HttpResponse<String> request = Unirest.put("https://api.spotify.com/v1/me/player")
				.header("Authorization", "Bearer " + tokenRepository.findOne(0).getAccessToken())
				.body("{\"device_ids\": [\"" + aliasRepository.findOne(alias.alias.trim()).deviceId + "\"]}")
				.asString();
		if (request.getStatus() != 204) {
			if (!retrying) {
				retrying = true;
				reauthenticate();
				transferPlayback(uuid, alias);
			}
			return "Cannot authenticate, try logging in again!";
		}
		retrying = false;
		return Integer.toString(request.getStatus());
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
}