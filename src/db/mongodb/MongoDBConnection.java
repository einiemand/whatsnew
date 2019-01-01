package db.mongodb;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;

import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;

public class MongoDBConnection implements DBConnection {

	private MongoClient mongoClient;
	private MongoDatabase db;

	public MongoDBConnection() {
		// Connects to local mongodb server.
		mongoClient = new MongoClient();
		db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
	}

	@Override
	public void close() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		db.getCollection("users").updateOne(new Document().append("user_id", userId), new Document().append("$push",
				new Document().append("favorite", new Document().append("$each", itemIds))));
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		db.getCollection("users").updateOne(new Document().append("user_id", userId),
				new Document().append("$pullAll", new Document().append("favorite", itemIds)));
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		Set<String> favoriteItemIds = new HashSet<>();
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		Document doc = iterable.first();
		if (doc != null && doc.containsKey("favorite")) {
			@SuppressWarnings("unchecked")
			List<String> itemIds = (List<String>) doc.get("favorite");
			favoriteItemIds.addAll(itemIds);
		}
		return favoriteItemIds;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		Set<Item> favoriteItems = new HashSet<>();

		Set<String> itemIds = getFavoriteItemIds(userId);
		for (String itemId : itemIds) {
			FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));
			if (iterable.first() != null) {
				Document doc = iterable.first();

				ItemBuilder builder = new ItemBuilder();
				builder.setItemId(itemId);
				builder.setName(doc.getString("name"));
				builder.setAddress(doc.getString("address"));
				builder.setUrl(doc.getString("url"));
				builder.setImageUrl(doc.getString("image_url"));
				builder.setRating(doc.getDouble("rating"));
				builder.setDistance(doc.getDouble("distance"));
				builder.setCategories(getCategories(itemId));

				favoriteItems.add(builder.build());
			}
		}
		return favoriteItems;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		Set<String> categories = new HashSet<>();
		FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));
		Document doc = iterable.first();
		if (doc != null && doc.containsKey("categories")) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) doc.get("categories");
			categories.addAll(list);
		}
		return categories;
	}

	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		TicketMasterAPI tmAPI = new TicketMasterAPI();
		List<Item> items = tmAPI.search(lat, lon, term);
		for (Item item : items) {
			saveItem(item);
		}
		return items;
	}

	@Override
	public void saveItem(Item item) {
		FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", item.getItemId()));

		if (iterable.first() == null) {
			db.getCollection("items")
					.insertOne(new Document().append("item_id", item.getItemId()).append("distance", item.getDistance())
							.append("name", item.getName()).append("address", item.getAddress())
							.append("url", item.getUrl()).append("image_url", item.getImageUrl())
							.append("rating", item.getRating()).append("categories", item.getCategories()));
		}
	}

	@Override
	public String getFullname(String userId) {
		String fullName = "";
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		Document doc = iterable.first();
		if (doc != null && doc.containsKey("first_name") && doc.containsKey("last_name")) {
			fullName += doc.getString("first_name") + " " + doc.getString("last_name");
		}
		return fullName;
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		Document doc = iterable.first();
		if (doc == null) {
			return false;
		}
		return doc.getString("password").equals(password);
	}

	@Override
	public boolean isUserIdUsed(String userId) {
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		return iterable.first() != null;
	}

	@Override
	public void registerNewUser(String userId, String password, String firstName, String lastName) {
		db.getCollection("users").insertOne(new Document().append("user_id", userId).append("password", password)
				.append("first_name", firstName).append("last_name", lastName));
	}

}
