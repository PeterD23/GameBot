package meetup.api;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import io.fusionauth.jwt.Signer;
import io.fusionauth.jwt.domain.JWT;
import io.fusionauth.jwt.rsa.RSASigner;

public class JWTGenerator {

	private static String signingKeyId = "oNaQUlOKh6mzzPTymhOQwXApCZ_qeQeDatOgQtzA52E";
	
	public static String encodeJwtWithRsa(String clientId) {
		
		try {
		// Get RSA signer using private key
		Signer signer = RSASigner.newSHA256Signer(new String(Files.readAllBytes(Paths.get("private.pem"))), signingKeyId);
		JWT jwt = new JWT().setAudience("api.meetup.com")
				.setIssuer(clientId)
				.setSubject("231389382")
				.setExpiration(ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(120));
		
		return JWT.getEncoder().encode(jwt, signer);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
}
