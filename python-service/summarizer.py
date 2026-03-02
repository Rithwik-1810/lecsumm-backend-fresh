from transformers import AutoModelForSeq2SeqLM, AutoTokenizer
import torch
import re

class Summarizer:
    def __init__(self):
        print("Loading summarization model...")
        self.model_name = "facebook/bart-large-cnn"
        self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)
        self.model = AutoModelForSeq2SeqLM.from_pretrained(self.model_name)
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model.to(self.device)
        self.model.eval()
        print("Summarization model loaded")

    def summarize(self, text, max_length=300, min_length=100):
        """
        Generate a detailed summary from the input text.
        Returns a string (the summary) and also a list of key points (all sentences of the summary).
        """
        if not text or len(text) < 200:
            return text, [text] if text else [], []  # fallback

        # Tokenize and generate summary
        inputs = self.tokenizer([text], max_length=1024, return_tensors="pt", truncation=True).to(self.device)
        summary_ids = self.model.generate(
            inputs["input_ids"],
            num_beams=4,
            max_length=max_length,
            min_length=min_length,
            early_stopping=True,
            no_repeat_ngram_size=3
        )
        summary = self.tokenizer.decode(summary_ids[0], skip_special_tokens=True)

        # Split summary into sentences to use as key points
        # Simple sentence splitting: split on '. ' and then add period back
        sentences = [s.strip() + '.' for s in summary.split('. ') if s.strip()]
        key_points = sentences if sentences else [summary]

        # Extract topics (simple capitalized word extraction)
        # Look for words that start with capital letters and are longer than 3 characters
        words = re.findall(r'\b[A-Z][a-z]{2,}\b', text)
        topics = list(set(words))[:5]  # limit to 5 topics

        return summary, key_points, topics