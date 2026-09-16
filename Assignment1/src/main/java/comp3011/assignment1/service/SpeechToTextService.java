package comp3011.assignment1.service;

// interface so the controller doesn't care whether it's the real openai call
// or the stub used for local testing
public interface SpeechToTextService {

	String transcribe(byte[] audio, String filename);
}
