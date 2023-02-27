package net.thesimpleteam.picohttp;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public record Client(Socket socket, BufferedOutputStream output, Map<String, String> headers) {

	public void send(int code, String codeMessage, ContentTypes contentType, String text) throws IOException {

		if (codeMessage == null) codeMessage = "Ok";
		if (contentType == null) contentType = ContentTypes.HTML;
		output.write(String.format(
				"HTTP/1.1 %d %s\nServer: PicoHTTP/0.1\nContent-type: %s\nContent-Length: %d\n\n%s",
				code, codeMessage, contentType.getContentType(), text.length(), text).getBytes(StandardCharsets.UTF_8));
		output.flush();
	}
}