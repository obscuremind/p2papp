package nemosofts.streambox.item.live;

import java.io.Serializable;

public class ItemLive implements Serializable {

	private final String name;
	private final String stream_id;
	private final String stream_icon;
	private boolean isFavorite;
	private String categoryId;

	public ItemLive(String name, String stream_id, String stream_icon) {
		this.name = name;
		this.stream_id = stream_id;
		this.stream_icon = stream_icon;
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
