from transformers import AutoModelForSeq2SeqLM, AutoTokenizer
import torch
import re

class Summarizer:
    def __init__(self):
        print("Loading summarization model (t5-small)...")
        self.model_name = "t5-small"
        self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)
        self.model = AutoModelForSeq2SeqLM.from_pretrained(self.model_name)
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model.to(self.device)
        self.model.eval()
        print("Summarization model loaded")

    def summarize(self, text, max_length=150, min_length=50):
        """Generate summary from text"""
        if not text or len(text) < 100:
            return text, [], []

        inputs = self.tokenizer([text], max_length=512, return_tensors="pt", truncation=True).to(self.device)
        summary_ids = self.model.generate(
            inputs["input_ids"],
            num_beams=4,
            max_length=max_length,
            min_length=min_length,
            early_stopping=True,
            no_repeat_ngram_size=3
        )
        summary = self.tokenizer.decode(summary_ids[0], skip_special_tokens=True)

        sentences = summary.split('. ')
        key_points = [s.strip() + '.' for s in sentences[:3] if s.strip()]
        if not key_points:
            key_points = [summary]

        words = re.findall(r'\b[A-Z][a-z]{2,}\b', text)
        topics = list(set(words))[:5]

        return summary, key_points, topics