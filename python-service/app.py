from flask import Flask, request, jsonify
from flask_cors import CORS
import os
import tempfile
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'healthy', 'message': 'Python AI Service is running'}), 200

@app.route('/process', methods=['POST'])
def process_lecture():
    try:
        # Check if file is present
        if 'file' not in request.files:
            return jsonify({'error': 'No file provided'}), 400

        file = request.files['file']
        language = request.form.get('language', 'english')
        extract_tasks = request.form.get('extractTasks', 'true').lower() == 'true'
        generate_summary = request.form.get('generateSummary', 'true').lower() == 'true'

        # Save file temporarily
        with tempfile.NamedTemporaryFile(delete=False, suffix=os.path.splitext(file.filename)[1]) as tmp_file:
            file.save(tmp_file.name)
            temp_path = tmp_file.name

        try:
            # For now, return mock data since Whisper is not installed
            logger.info(f"Processing file: {file.filename}")
            
            # Mock transcript
            transcript = f"This is a simulated transcript for the lecture: {file.filename}. The lecture covers important topics in the field."

            response = {
                'transcript': transcript,
                'summary': None,
                'tasks': []
            }

            # Mock summary for testing
            if generate_summary:
                response['summary'] = {
                    'content': "This lecture covers key concepts and important topics. The main points include understanding the fundamentals and applying them in practice.",
                    'keyPoints': [
                        "Key concept 1: Understanding the basics",
                        "Key concept 2: Practical applications",
                        "Key concept 3: Advanced techniques"
                    ],
                    'topics': ["Machine Learning", "Neural Networks", "Deep Learning"],
                    'confidence': 85
                }

            # Mock tasks for testing
            if extract_tasks:
                response['tasks'] = [
                    {
                        'title': 'Complete assignment',
                        'description': 'Finish the homework problems',
                        'priority': 'High',
                        'deadline': '2024-03-01'
                    },
                    {
                        'title': 'Read chapter 5',
                        'description': 'Study the recommended reading',
                        'priority': 'Medium',
                        'deadline': '2024-02-28'
                    }
                ]

            return jsonify(response), 200

        finally:
            # Clean up temp file
            os.unlink(temp_path)

    except Exception as e:
        logger.error(f"Error processing lecture: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/summarize', methods=['POST'])
def summarize():
    try:
        data = request.json
        text = data.get('text', '')
        
        if not text:
            return jsonify({'error': 'No text provided'}), 400

        return jsonify({
            'content': text[:200] + "..." if len(text) > 200 else text,
            'keyPoints': ["Summary point 1", "Summary point 2", "Summary point 3"],
            'topics': ["Topic A", "Topic B", "Topic C"],
            'confidence': 80
        }), 200

    except Exception as e:
        logger.error(f"Error summarizing: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/extract-tasks', methods=['POST'])
def extract_tasks():
    try:
        data = request.json
        text = data.get('text', '')

        if not text:
            return jsonify({'error': 'No text provided'}), 400

        return jsonify({
            'tasks': [
                {
                    'title': 'Task 1',
                    'description': 'Complete the first task',
                    'priority': 'High',
                    'deadline': '2024-03-01'
                },
                {
                    'title': 'Task 2',
                    'description': 'Complete the second task',
                    'priority': 'Medium',
                    'deadline': '2024-02-28'
                }
            ]
        }), 200

    except Exception as e:
        logger.error(f"Error extracting tasks: {str(e)}")
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    print("=" * 50)
    print("🚀 Python AI Service Starting...")
    print("📍 http://localhost:5001")
    print("=" * 50)
    app.run(host='0.0.0.0', port=5001, debug=True)