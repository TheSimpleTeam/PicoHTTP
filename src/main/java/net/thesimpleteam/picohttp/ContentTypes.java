package net.thesimpleteam.picohttp;

public enum ContentTypes {

	//Text
	HTML(Types.TEXT),
	PLAIN(Types.TEXT),
	CSS(Types.TEXT),
	XML(Types.TEXT),
	CSV(Types.TEXT),
	//Applications
	ZIP(Types.APPLICATION),
	JSON(Types.APPLICATION),
	BINARY(Types.APPLICATION, "octet-stream"),
	JAR(Types.APPLICATION, "java-archive"),
	JS(Types.APPLICATION, "javascript"),
	PDF(Types.APPLICATION),
	//Video
	MP4(Types.VIDEO),
	//Image
	PNG(Types.IMAGE),
	GIF(Types.IMAGE),
	JPEG(Types.IMAGE),
	//Audio
	MPEG(Types.AUDIO),
	//Multipart
	MIXED(Types.MULTIPART),
	ALTERATIVE(Types.MULTIPART),
	RELATED(Types.MULTIPART),
	;

	private enum Types {
		MULTIPART,
		IMAGE,
		VIDEO,
		APPLICATION,
		TEXT,
		AUDIO;
	}

	private final String contentType;

	ContentTypes(Types type) {
		this.contentType = type.name().toLowerCase() + "/" + this.name().toLowerCase();
	}

	ContentTypes(Types type, String override) {
		this.contentType = type.name().toLowerCase() + "/" + override;
	}
	
	public String getContentType() {
		return this.contentType;
	}
}
