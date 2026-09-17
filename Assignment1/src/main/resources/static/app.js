const recordButton = document.getElementById("recordButton");
const statusText = document.getElementById("status");
const transcript = document.getElementById("transcript");

let mediaRecorder = null;
let chunks = [];

recordButton.addEventListener("click", () => {
	if (mediaRecorder && mediaRecorder.state === "recording") {
		mediaRecorder.stop();
	} else {
		startRecording();
	}
});

async function startRecording() {
	// browser blocks getUserMedia unless the page is on localhost or https
	if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
		setStatus("This browser can't record audio.", "error");
		return;
	}

	let stream;
	try {
		stream = await navigator.mediaDevices.getUserMedia({ audio: true });
	} catch (err) {
		// usually the user clicked block, or there's no microphone
		setStatus("Could not use the microphone. Check the permission and try again.", "error");
		return;
	}

	chunks = [];
	mediaRecorder = new MediaRecorder(stream);

	mediaRecorder.addEventListener("dataavailable", (event) => {
		if (event.data.size > 0) {
			chunks.push(event.data);
		}
	});

	mediaRecorder.addEventListener("stop", async () => {
		// let go of the mic so the browser stops showing the recording indicator
		stream.getTracks().forEach((track) => track.stop());
		await sendRecording(new Blob(chunks, { type: mediaRecorder.mimeType }));
	});

	mediaRecorder.start();
	setStatus("Recording... press stop when you're done.", "recording");
	recordButton.textContent = "Stop recording";
	recordButton.classList.add("recording");
}

async function sendRecording(blob) {
	setStatus("Transcribing...");
	recordButton.textContent = "Start recording";
	recordButton.classList.remove("recording");
	recordButton.disabled = true;

	// filename matters, openai works out the audio format from the extension
	const form = new FormData();
	form.append("audio", blob, "recording.webm");

	try {
		const response = await fetch("/api/v1/transcriptions", {
			method: "POST",
			body: form
		});

		if (!response.ok) {
			const error = await response.json().catch(() => null);
			setStatus(error && error.message ? error.message : "Transcription failed.", "error");
			return;
		}

		const result = await response.json();
		transcript.textContent = result.text;
		setStatus("Done. Ready for another recording.");
	} catch (err) {
		// fetch only throws for network level problems, not for 4xx/5xx
		setStatus("Could not reach the server.", "error");
	} finally {
		// always go back to a state where a new recording can start
		recordButton.disabled = false;
	}
}

function setStatus(message, kind) {
	statusText.textContent = message;
	statusText.className = kind ? "status " + kind : "status";
}
