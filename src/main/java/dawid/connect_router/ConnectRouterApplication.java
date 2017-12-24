package dawid.connect_router;

import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

@SpringBootApplication
public class ConnectRouterApplication {

	@Getter
	private static UUID uuid;

	public static void main(String[] args) throws Exception {
		Path uuidFilePath = Paths.get("spotify_connect_router_UUID");
		if (Files.exists(uuidFilePath)) {
			uuid = UUID.fromString(Files.readAllLines(uuidFilePath).get(0));
		}
		else {
			uuid = UUID.randomUUID();
			Files.write(uuidFilePath, Collections.singleton(uuid.toString()));
		}
		SpringApplication.run(ConnectRouterApplication.class, args);
	}
}
