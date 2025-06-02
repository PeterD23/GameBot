package meetup.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gamebot.ChannelLogger;

@SuppressWarnings("resource")
public class MeetupApiQuerier {

	private ObjectMapper objectMapper;

	private String clientId;
	private String clientSecret;
	private String refreshToken;

	private String queryString = "query($eventId: ID) { event(id: $eventId) { title description dateTime eventUrl tickets { edges { node { user { name id } } } } } }";

	public MeetupApiQuerier() {
		objectMapper = new ObjectMapper();
		try {
			MeetupConfig config = objectMapper.readValue(new File("meetup.json"), MeetupConfig.class);
			clientId = config.getClientId();
			clientSecret = config.getClientSecret();
			refreshToken = config.getRefreshToken();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public JwtDTO generateApiToken() {
		Exception firstExc = null;
		Exception secondExc = null;
		// Try with Refresh Token
		try {
			JwtDTO token = getJwtRefresh(refreshToken);
			return token;
		} catch (Exception e) {
			firstExc = e;
			ChannelLogger.logMessageError("Unable to generate refresh token.", firstExc);
		}
		
		// Try with Private Key
		try {
			JwtDTO token = getJwt();
			refreshToken = token.getRefreshToken();
			return token;
		} catch (Exception e) {
			secondExc = e;
			ChannelLogger.logMessageError("Unable to generate JWT Token.", secondExc);
		}
		
		ChannelLogger.logHighPriorityMessage("Unable to generate token with both methods", null);
		return null;
	}
	
	public MeetupApiResponse getEventDetails(JwtDTO token, String eventId) {
		try {
			String response = makeGraphQuery(token, eventId);
			JsonNode root = objectMapper.readTree(response);
			MeetupApiResponse event = new MeetupApiResponse(root);
			return event;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public JwtDTO getJwt() throws IOException {

		String signedJwt = JWTGenerator.encodeJwtWithRsa(clientId);
		System.out.println(signedJwt);

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost("https://secure.meetup.com/oauth2/access");

		List<BasicNameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer"));
		params.add(new BasicNameValuePair("assertion", signedJwt));

		httpPost.setEntity(new UrlEncodedFormEntity(params));
		
		String content = execute(httpClient, httpPost);
		JwtDTO jwt = objectMapper.readValue(content, JwtDTO.class); 
		httpClient.close();
		
		return jwt;
	}

	public JwtDTO getJwtRefresh(String refreshToken) throws IOException {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost("https://secure.meetup.com/oauth2/access");

		List<BasicNameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("client_id", clientId));
		params.add(new BasicNameValuePair("client_secret", clientSecret));
		params.add(new BasicNameValuePair("grant_type", "refresh_token"));
		params.add(new BasicNameValuePair("refresh_token", refreshToken));

		httpPost.setEntity(new UrlEncodedFormEntity(params));
		
		String content = execute(httpClient, httpPost);
		JwtDTO jwt = objectMapper.readValue(content, JwtDTO.class); 
		httpClient.close();
		
		return jwt;
	}

	public String makeGraphQuery(JwtDTO token, String eventId) throws Exception {

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost("https://api.meetup.com/gql");
		httpPost.addHeader("Authorization", "Bearer " + token.getAccessToken());
		httpPost.addHeader("Content-Type", "application/json");

		GraphRequestDTO request = new GraphRequestDTO(queryString, eventId);
		String requestAsJson = objectMapper.writeValueAsString(request);
		HttpEntity stringEntity = new StringEntity(requestAsJson, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);

		String content = execute(httpClient, httpPost); 
		httpClient.close();
		
		return content;
	}
	
	private String execute(CloseableHttpClient client, HttpPost post) {
		try {
			CloseableHttpResponse response = client.execute(post);
			HttpEntity entity = response.getEntity();
			String details = EntityUtils.toString(entity);
			response.close();
			return details;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
