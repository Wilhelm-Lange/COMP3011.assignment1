package comp3011.assignment1.web;

// the upload itself was wrong (missing or empty file) -> 400
public class BadUploadException extends RuntimeException {

	public BadUploadException(String message) {
		super(message);
	}
}
