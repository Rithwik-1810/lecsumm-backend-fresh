from flask import Flask, request, jsonify
from flask_cors import CORS
import os
import uuid
import subprocess
import json
from werkzeug.utils import secure_filename

from speech_to_text import SpeechToText
from summarizer import Summarizer
from task_extractor import TaskExtractor

app = Flask(__name__)
CORS(app)

UPLOAD_FOLDER = 'uploads'
ALLOWED_EXTENSIONS = {'mp3', 'wav', 'm4a', 'ogg', 'flac', 'mp4', 'avi', 'mov'}
MAX_CONTENT_LENGTH = 500 * 1024 * 1024  # 500MB

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = MAX_CONTENT_LENGTH

os.makedirs(UPLOAD_FOLDER, exist_ok=True)

stt = SpeechToText()
summarizer = Summarizer()
task_extractor = TaskExtractor()

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def get_audio_duration(filepath):
    """Use ffprobe to get duration in seconds"""
    try:
        result = subprocess.run(
            ['ffprobe', '-v', 'quiet', '-print_format', 'json', '-show_format', filepath],
            capture_output=True,
            text=True,
            check=True
        )
        info = json.loads(result.stdout)
        return float(info['format']['duration'])
    except Exception as e:
        print(f"Error getting duration: {e}")
        return 0.0

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok"})

@app.route('/process', methods=['POST'])
def process_lecture():
    try:
        if 'file' not in request.files:
            return jsonify({"error": "No file provided"}), 400

        file = request.files['file']
        if file.filename == '':
            return jsonify({"error": "Empty filename"}), 400

        if not allowed_file(file.filename):
            return jsonify({"error": "File type not allowed"}), 400

        language = request.form.get('language', 'english')
        extract_tasks = request.form.get('extractTasks', 'true').lower() == 'true'
        generate_summary = request.form.get('generateSummary', 'true').lower() == 'true'

        filename = secure_filename(f"{uuid.uuid4()}_{file.filename}")
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        file.save(filepath)

        print(f"Processing file: {filename}")

        # Get duration
        duration_seconds = get_audio_duration(filepath)
        print(f"Duration: {duration_seconds} seconds")

        # Transcribe
        transcript = stt.transcribe(filepath, language)
        if not transcript:
            return jsonify({"error": "Transcription failed"}), 500

        # Summarize
        summary_data = None
        if generate_summary:
            summary_text, key_points, topics = summarizer.summarize(transcript)
            summary_data = {
                "content": summary_text,
                "keyPoints": key_points,
                "topics": topics,
                "confidence": 85
            }

        # Extract tasks
        tasks = []
        if extract_tasks:
            tasks = task_extractor.extract_tasks(transcript)

        # Clean up uploaded file (optional)
        os.remove(filepath)

        response = {
            "transcript": transcript,
            "summary": summary_data,
            "tasks": tasks,
            "durationSeconds": duration_seconds   # <-- NEW field
        }

        return jsonify(response)

    except Exception as e:
        print(f"Error in process_lecture: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)