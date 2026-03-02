import re
from datetime import datetime, timedelta
import random

class TaskExtractor:
    def __init__(self):
        # Common task-related keywords
        self.task_keywords = [
            'assignment', 'homework', 'project', 'quiz', 'exam', 'test',
            'read', 'study', 'review', 'complete', 'submit', 'prepare',
            'write', 'create', 'analyze', 'research', 'present'
        ]
        
        # Deadline patterns
        self.deadline_patterns = [
            r'due (\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?)',
            r'deadline (\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?)',
            r'by (\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?)',
            r'on (\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?)',
            r'before (\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?)',
        ]

    def extract_tasks(self, text):
        """Extract tasks and deadlines from text"""
        tasks = []
        sentences = re.split(r'[.!?]+', text)
        
        for sentence in sentences:
            sentence = sentence.strip()
            if len(sentence) < 15:  # Skip very short sentences
                continue
                
            # Check if sentence contains task keywords
            task_keywords_found = [kw for kw in self.task_keywords if kw in sentence.lower()]
            if not task_keywords_found:
                continue
                
            # Determine priority based on keywords
            priority = "Medium"
            if any(kw in sentence.lower() for kw in ['important', 'urgent', 'critical', 'asap']):
                priority = "High"
            elif any(kw in sentence.lower() for kw in ['optional', 'if time', 'bonus']):
                priority = "Low"
                
            # Extract deadline
            deadline = None
            for pattern in self.deadline_patterns:
                match = re.search(pattern, sentence, re.IGNORECASE)
                if match:
                    deadline_str = match.group(1)
                    # Try to parse the date (simplified)
                    try:
                        # If no year, assume current year
                        parts = re.split(r'[/-]', deadline_str)
                        if len(parts) == 2:
                            month, day = parts
                            year = datetime.now().year
                            deadline_date = datetime.strptime(f"{year}-{month}-{day}", "%Y-%m-%d")
                            if deadline_date < datetime.now():
                                deadline_date = deadline_date.replace(year=year+1)
                            deadline = deadline_date.strftime("%Y-%m-%d")
                        elif len(parts) == 3:
                            # Try different orders
                            for order in [(0,1,2), (2,0,1), (2,1,0)]:
                                try:
                                    year, month, day = [int(parts[i]) for i in order]
                                    if year < 100:
                                        year += 2000
                                    deadline_date = datetime(year, month, day)
                                    deadline = deadline_date.strftime("%Y-%m-%d")
                                    break
                                except:
                                    pass
                    except:
                        pass
                    if deadline:
                        break
            
            # If no deadline found, set a default 7 days from now
            if not deadline:
                deadline = (datetime.now() + timedelta(days=7)).strftime("%Y-%m-%d")
                
            # Truncate title to first 60 chars
            title = sentence[:60] + ('...' if len(sentence) > 60 else '')
            
            tasks.append({
                "title": title,
                "description": sentence,
                "priority": priority,
                "deadline": deadline
            })
        
        # Remove duplicates (keep first occurrence)
        unique_tasks = []
        seen_titles = set()
        for task in tasks:
            if task['title'] not in seen_titles:
                seen_titles.add(task['title'])
                unique_tasks.append(task)
        
        return unique_tasks