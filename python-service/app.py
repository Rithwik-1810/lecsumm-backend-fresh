from flask import Flask, request, jsonify
from flask_cors import CORS
import os
import uuid
import subprocess
import json
import logging
from werkzeug.utils import secure_filename

from speech_to_text import SpeechToText
from summarizer import Summarizer
from task_extractor import TaskExtractor

# Configure logging to show everything
logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# Use /tmp for writable storage on Hugging Face Spaces
UPLOAD_FOLDER = '/tmp/uploads'
ALLOWED_EXTENSIONS = {'mp3', 'wav', 'm4a', 'ogg', 'flac', 'mp4', 'avi', 'mov'}
MAX_CONTENT_LENGTH = 500 * 1024 * 1024  # 500MB

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = MAX_CONTENT_LENGTH

os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# Initialize models with error handling
logger.info("Initializing models...")
try:
    stt = SpeechToText()
    summarizer = Summarizer()
    task_extractor = TaskExtractor()
except Exception as e:
    logger.exception("Failed to initialize models")
    raise

logger.info("All models initialized successfully")

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def get_audio_duration(filepath):
    """Use ffprobe to get duration in seconds. Returns 0 on failure."""
    try:
        result = subprocess.run(
            ['ffprobe', '-v', 'quiet', '-print_format', 'json', '-show_format', filepath],
            capture_output=True,
            text=True,
            check=True,
            timeout=30
        )
        info = json.loads(result.stdout)
        return float(info['format']['duration'])
    except Exception as e:
        logger.warning(f"Could not get duration for {filepath}: {e}")
        return 0.0

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok"})

@app.route('/process', methods=['POST'])
def process_lecture():
    try:
        logger.info("Received /process request")
        if 'file' not in request.files:
            logger.error("No file part in request")
            return jsonify({"error": "No file provided"}), 400

        file = request.files['file']
        if file.filename == '':
            logger.error("Empty filename")
            return jsonify({"error": "Empty filename"}), 400

        if not allowed_file(file.filename):
            logger.error(f"File type not allowed: {file.filename}")
            return jsonify({"error": "File type not allowed"}), 400

        language = request.form.get('language', 'english')
        extract_tasks = request.form.get('extractTasks', 'true').lower() == 'true'
        generate_summary = request.form.get('generateSummary', 'true').lower() == 'true'

        # Save file to temporary location
        filename = secure_filename(f"{uuid.uuid4()}_{file.filename}")
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        file.save(filepath)
        logger.info(f"File saved: {filename}, size: {os.path.getsize(filepath)} bytes")

        # Get duration
        duration = get_audio_duration(filepath)
        logger.info(f"Audio duration: {duration} seconds")

        # Step 1: Transcribe
        try:
            logger.info("Starting transcription...")
            transcript = stt.transcribe(filepath, language[:2])  # whisper uses 'en', 'hi', etc.
            if not transcript:
                raise Exception("Transcription returned None")
            logger.info(f"Transcription successful, length: {len(transcript)} chars")
        except Exception as e:
            logger.exception("Transcription failed")
            # Clean up file before returning error
            try:
                os.remove(filepath)
            except:
                pass
            return jsonify({"error": "Transcription failed", "details": str(e)}), 500

        # Step 2: Summarize (if requested)
        summary_data = None
        if generate_summary:
            try:
                logger.info("Generating summary...")
                summary_text, key_points, topics = summarizer.summarize(transcript)
                summary_data = {
                    "content": summary_text,
                    "keyPoints": key_points,
                    "topics": topics,
                    "confidence": 85
                }
                logger.info(f"Summary generated: {len(summary_text)} chars, {len(key_points)} key points")
            except Exception as e:
                logger.exception("Summarization failed")
                # Continue without summary

        # Step 3: Extract tasks (if requested)
        tasks = []
        if extract_tasks:
            try:
                logger.info("Extracting tasks...")
                tasks = task_extractor.extract_tasks(transcript)
                logger.info(f"Extracted {len(tasks)} tasks")
            except Exception as e:
                logger.exception("Task extraction failed")

        # Clean up uploaded file
        try:
            os.remove(filepath)
            logger.info(f"Deleted temporary file: {filename}")
        except Exception as e:
            logger.warning(f"Could not delete file {filename}: {e}")

        # Build response
        response = {
            "transcript": transcript,
            "summary": summary_data,
            "tasks": tasks,
            "durationSeconds": duration
        }
        logger.info("Returning successful response")
        return jsonify(response)

    except Exception as e:
        logger.exception("Unhandled exception in /process")
        return jsonify({"error": "Internal server error", "details": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=7860, debug=True)