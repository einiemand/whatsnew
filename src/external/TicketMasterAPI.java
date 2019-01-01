package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;

public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = ""; // no restriction
	private static final String API_KEY = "wxUiHZohA4yFmFhCxPVDce8zRPx7y1tz";

	/**
	 * @param lat
	 * @param lon
	 * @param keyword
	 * @return
	 */
	public List<Item> search(double lat, double lon, String keyword) {
		if (keyword == null) {
			keyword = DEFAULT_KEYWORD;
		}
		try {
			keyword = URLEncoder.encode(keyword, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String geoHash = GeoHash.encodeGeohash(lat, lon, 8);

		// apikey=abcde&geoPoint=xyz123&keyword=&radius=50
		String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s", API_KEY, geoHash, keyword, 50);
		String url = URL + "?" + query;
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("GET");
			int responseCode = connection.getResponseCode();
			System.out.println(url);
			System.out.println(responseCode);
			if (responseCode != 200) {
				return new ArrayList<>();
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			StringBuilder response = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
			reader.close();
			JSONObject obj = new JSONObject(response.toString());
			if (obj.has("_embedded")) {
				JSONObject embedded = obj.getJSONObject("_embedded");
				return getItemList(embedded.getJSONArray("events"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<Item>();
		for (int i = 0; i < events.length(); ++i) {
			JSONObject event = events.getJSONObject(i);
			ItemBuilder builder = new ItemBuilder();
			if (event.has("id")) {
				builder.setItemId(event.getString("id"));
			}
			if (event.has("name")) {
				builder.setName(event.getString("name"));
			}
			if (event.has("url")) {
				builder.setUrl(event.getString("url"));
			}
			if (event.has("distance")) {
				builder.setDistance(event.getDouble("distance"));
			}
			if (event.has("rating")) {
				builder.setRating(event.getDouble("rating"));
			}
			builder.setAddress(getAddress(event));
			builder.setCategories(getCategories(event));
			builder.setImageUrl(getImageUrl(event));
			itemList.add(builder.build());
		}
		return itemList;
	}

	private String getAddress(JSONObject event) throws JSONException {
		if (event.has("_embedded")) {
			JSONObject embedded = event.getJSONObject("_embedded");
			if (embedded.has("venues")) {
				JSONArray venues = embedded.getJSONArray("venues");
				for (int i = 0; i < venues.length(); ++i) {
					JSONObject venue = venues.getJSONObject(i);
					StringBuilder addressBuilder = new StringBuilder();
					if (venue.has("address")) {
						JSONObject address = venue.getJSONObject("address");
						if (address.has("line1")) {
							addressBuilder.append(address.getString("line1"));
						}
						if (address.has("line2")) {
							addressBuilder.append(", ");
							addressBuilder.append(address.getString("line2"));
						}
						if (address.has("line3")) {
							addressBuilder.append(", ");
							addressBuilder.append(address.getString("line3"));
						}
					}
					if (venue.has("city")) {
						JSONObject city = venue.getJSONObject("city");
						if (city.has("name")) {
							addressBuilder.append(",");
							addressBuilder.append(city.getString("name"));
						}
					}
					String addressString = addressBuilder.toString();
					if (!addressString.equals("")) {
						return addressString;
					}
				}
			}
		}
		return "";
	}

	private Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>();
		if (event.has("classifications")) {
			JSONArray classifications = event.getJSONArray("classifications");
			for (int i = 0; i < classifications.length(); ++i) {
				JSONObject classification = classifications.getJSONObject(i);
				if (classification.has("segment")) {
					JSONObject segment = classification.getJSONObject("segment");
					if (segment.has("name")) {
						categories.add(segment.getString("name"));
					}
				}
			}
		}
		return categories;
	}

	private String getImageUrl(JSONObject event) throws JSONException {
		if (event.has("images")) {
			JSONArray array = event.getJSONArray("images");
			for (int i = 0; i < array.length(); ++i) {
				JSONObject image = array.getJSONObject(i);
				if (image.has("url")) {
					return image.getString("url");
				}
			}
		}
		return "";
	}

	private void queryAPI(double lat, double lon) {
		List<Item> events = search(lat, lon, null);

		for (Item event : events) {
			System.out.println(event.toJSONObject());
		}
	}

	public static void main(String[] args) {
		TicketMasterAPI tmAPI = new TicketMasterAPI();
		tmAPI.queryAPI(37.38, -122.08);
	}

}
