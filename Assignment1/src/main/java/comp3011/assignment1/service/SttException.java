package comp3011.assignment1.service;

// something went wrong talking to openai
public class SttException extends RuntimeException {

	public SttException(String message) {
		super(message);
	}

	public SttException(String message, Throwable cause) {
		super(message, cause);
	}
}
