import whisper
import os
from pydub import AudioSegment

class SpeechToText:
    def __init__(self):
        print("Loading Whisper model...")
        self.model = whisper.load_model("base")  # Use "tiny" for faster but less accurate
        print("Whisper model loaded")

    def transcribe(self, audio_path, language="english"):
        """Transcribe audio file using Whisper"""
        try:
            # Convert to wav if needed (Whisper handles many formats)
            result = self.model.transcribe(audio_path, language=language[:2])  # Whisper uses 2-letter codes
            return result["text"]
        except Exception as e:
            print(f"Transcription error: {e}")
            return None