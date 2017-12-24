package dawid.connect_router;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class Token {
	@Id
	private int id;

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("token_type")
	private String tokenType;

	private String scope;

	@JsonProperty("expires_in")
	private int expiresIn;

	@JsonProperty("refresh_token")
	private String refreshToken;

	public Token() {
		//There should be only one
		id = 0;
	}
}
