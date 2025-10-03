package nemosofts.streambox.item.movie;

import java.io.Serializable;

public class ItemMovies implements Serializable {

	private final String name;
	private final String stream_id;
	private final String stream_icon;
	private final String rating;
	private boolean isFavorite;
	private String categoryId;

	public ItemMovies(String name, String stream_id, String stream_icon, String rating) {
		this.name = name;
		this.stream_id = stream_id;
		this.stream_icon = stream_icon;
		this.rating = rating;
		this.isFavorite = false;
		this.categoryId = "";
	}

	public String getName() {
		return name;
	}

	public String getStreamID() {
		return stream_id;
	}

	public String getStreamIcon() {
		return stream_icon;
	}

	public String getRating() {
		return rating;
	}

	public boolean isFavorite() {
		return isFavorite;
	}

	public void setFavorite(boolean favorite) {
		isFavorite = favorite;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

}
