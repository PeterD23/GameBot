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

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gamebot.ChannelLogger;

@SuppressWarnings("resource")
public class MeetupApiQuerier {

	private ObjectMapper objectMapper;

	private String clientId;
	private String clientSecret;
	private String refreshToken;

	private String eventQueryString = "query($eventId: ID!) { event(id: $eventId) { id title description dateTime eventUrl maxTickets featuredEventPhoto{highResUrl} venues{name address city lat lon} rsvps(first:100, filter: {rsvpStatus:[YES, WAITLIST, NO]}) { waitlistCount yesCount noCount edges { node { status member { name id } } } } } }";
	private String groupQueryString = "query { groupByUrlname(urlname: \"edinburgh-local-video-gamers\") { events(first:10) { edges { node { id } } } } }";

	private String getMemberNameFromId = "query{ groupByUrlname(urlname: \"edinburgh-local-video-gamers\") { memberships(filter:{memberIds:[$memberId]}) { edges { node { id name memberPhoto {thumbUrl} } } } } }";

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

	public ArrayList<String> getUpcomingEvents(JwtDTO token) throws Exception {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost("https://api.meetup.com/gql-ext");
		httpPost.addHeader("Authorization", "Bearer " + token.getAccessToken());
		httpPost.addHeader("Content-Type", "application/json");

		EventGraphRequestDTO request = new EventGraphRequestDTO(groupQueryString, "");
		String requestAsJson = objectMapper.writeValueAsString(request);
		HttpEntity stringEntity = new StringEntity(requestAsJson, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);

		String content = execute(httpClient, httpPost);
		httpClient.close();

		JsonNode root = objectMapper.readTree(content);

		ArrayList<String> eventIds = new ArrayList<>();
		TreeNode events = root.at("/data/groupByUrlname/events/edges");
		for (int i = 0; i < events.size(); i++) {
			eventIds.add(events.path(i).at("/node/id").toString().replaceAll("\"", ""));
		}
		return eventIds;
	}

	public String[] getNameAndImageOfUser(JwtDTO token, String id) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpPost httpPost = new HttpPost("https://api.meetup.com/gql-ext");
			httpPost.addHeader("Authorization", "Bearer " + token.getAccessToken());
			httpPost.addHeader("Content-Type", "application/json");

			EventGraphRequestDTO request = new EventGraphRequestDTO(getMemberNameFromId.replace("$memberId", id));
			String requestAsJson = objectMapper.writeValueAsString(request);
			HttpEntity stringEntity = new StringEntity(requestAsJson, ContentType.APPLICATION_JSON);
			httpPost.setEntity(stringEntity);

			String content = execute(httpClient, httpPost);
			httpClient.close();

			JsonNode root = objectMapper.readTree(content);
			String name = root.at("/data/groupByUrlname/memberships/edges").path(0).at("/node/name").toString()
					.replaceAll("\"", "");
			String image = root.at("/data/groupByUrlname/memberships/edges").path(0).at("/node/memberPhoto/thumbUrl").toString()
					.replaceAll("\"", "");
			return new String[] { name, image };
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String makeGraphQuery(JwtDTO token, String variable) throws Exception {

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost("https://api.meetup.com/gql-ext");
		httpPost.addHeader("Authorization", "Bearer " + token.getAccessToken());
		httpPost.addHeader("Content-Type", "application/json");

		EventGraphRequestDTO request = new EventGraphRequestDTO(eventQueryString, variable);
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
