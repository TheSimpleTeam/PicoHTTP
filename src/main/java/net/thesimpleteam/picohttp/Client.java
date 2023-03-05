package net.thesimpleteam.picohttp;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
* @param socket The socket of the HTTP client.
* @param output The output stream of the HTTP client, it's used to send the data. @see Client#send(int, String, ContentTypes, String).
* @param method The HTTP method used, for example GET or POST.
* @param headers A map containing the headers sent by the HTTP client.
* @param data The data sent by the HTTP client, it might be null if no data has been sent (eg: when it's a GET request).
* @param path The path used by the HTTP client, it always starts with a {@code /} (eg: /addMember/minemobs).
*/
public record Client(Socket socket, BufferedOutputStream output, String method, Map<String, String> headers, String data, String path) {

	/**
	* It uses the {@link Client#send(int, String, ContentTypes, String)} and sets the ContentType to {@link ContentTypes#PLAIN} and the text to the codeMessage parameter.
	*/
	public void send(int code, String codeMessage) throws IOException {
		this.send(code, codeMessage, ContentTypes.PLAIN, codeMessage);
	}
	
	public void send(int code, String codeMessage, ContentTypes contentType, String text) throws IOException {
		if (codeMessage == null) codeMessage = "Ok";
		if (contentType == null) contentType = ContentTypes.HTML;
		if(socket.isClosed()) return;
		output.write(String.format(
				"HTTP/1.1 %d %s\nServer: PicoHTTP/%s\nContent-type: %s\nContent-Length: %d\n\n%s",
				code, codeMessage, PicoHTTP.VERSION, contentType.getContentType(), text.length(), text).getBytes(StandardCharsets.UTF_8));
		output.flush();
	}
}