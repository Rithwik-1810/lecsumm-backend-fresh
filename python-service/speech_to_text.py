from faster_whisper import WhisperModel

class SpeechToText:
    def __init__(self, model_size="base"):
        print(f"Loading Faster-Whisper model ({model_size})...")
        self.model = WhisperModel(model_size, device="cpu", compute_type="int8")
        print("Faster-Whisper model loaded")

    def transcribe(self, audio_path, language="en"):
        try:
            segments, info = self.model.transcribe(audio_path, language=language, beam_size=5)
            text = " ".join([segment.text for segment in segments])
            return text
        except Exception as e:
            print(f"Transcription error: {e}")
            return None