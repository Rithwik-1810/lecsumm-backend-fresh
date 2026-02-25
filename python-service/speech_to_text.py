import whisper

class SpeechToText:
    def __init__(self, model_size="base"):
        self.model = whisper.load_model(model_size)
    
    def transcribe(self, audio_path, language=None):
        result = self.model.transcribe(audio_path, language=language)
        return result['text']