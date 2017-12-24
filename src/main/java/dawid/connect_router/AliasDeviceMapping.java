package dawid.connect_router;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class AliasDeviceMapping {
	@Id
	String alias;

	String deviceId;
}
