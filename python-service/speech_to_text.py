from faster_whisper import WhisperModel
import logging

logger = logging.getLogger(__name__)

class SpeechToText:
    def __init__(self, model_size="tiny.en"):  # Use smallest model to save memory
        logger.info(f"Loading Faster-Whisper model '{model_size}'...")
        try:
            self.model = WhisperModel(model_size, device="cpu", compute_type="int8")
            logger.info("Faster-Whisper model loaded successfully")
        except Exception as e:
            logger.exception("Failed to load Whisper model")
            raise

    def transcribe(self, audio_path, language="en"):
        """
        Transcribe audio file using Faster-Whisper.
        Returns transcribed text or None on failure.
        """
        try:
            logger.info(f"Transcribing {audio_path} with language '{language}'")
            segments, info = self.model.transcribe(audio_path, language=language, beam_size=5)
            text = " ".join([segment.text for segment in segments])
            logger.info(f"Transcription completed, got {len(text)} characters")
            return text
        except Exception as e:
            logger.exception("Error during transcription")
            return None