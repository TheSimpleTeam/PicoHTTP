package net.thesimpleteam.picohttp;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public record Client(Socket socket, BufferedOutputStream output) {

	public void send(int code, String codeMessage, ContentTypes contentType, String text) {
		try {
			if (codeMessage == null) codeMessage = "Ok";
			if (contentType == null) contentType = ContentTypes.HTML;
			output.write(String.format(
					"HTTP/1.0 %d %s\nServer: PicoHTTP/0.1\nContent-type: %s\nContent-Length: %d\n\n%s",
					code, codeMessage, contentType.getContentType(), text.length(), text).getBytes(StandardCharsets.UTF_8));
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}