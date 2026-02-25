import re

class TaskExtractor:
    def __init__(self):
        pass
    
    def extract(self, text):
        # Simple task extraction (mock)
        return [
            {
                'title': 'Sample Task 1',
                'description': 'Complete the assignment',
                'priority': 'High',
                'deadline': '2024-03-01'
            },
            {
                'title': 'Sample Task 2',
                'description': 'Read chapter 5',
                'priority': 'Medium',
                'deadline': '2024-02-28'
            }
        ]