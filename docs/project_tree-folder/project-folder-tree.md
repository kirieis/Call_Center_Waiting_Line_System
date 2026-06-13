# Project Folder Tree - Call Center Waiting Line System

## 📁 Complete Directory Structure

Call_Center_Waiting_Line_System-Group7/
│
├── 📄 README.md
├── 📄 .gitignore
│
│
├── 📁 src/                              # Source code directory
│   ├── 📁 config/                       # Configuration files
│   │   ├── 📄 ConfigLoader.java         # Configuration loader
│   │   └── 📄 settings.properties       # Settings configuration
│   │
│   ├── 📁 core/                         # Core business logic
│   │   ├── 📄 AgingAlgorithm.java       # Aging algorithm implementation
│   │   ├── 📄 CallProcessor.java        # Call processing logic
│   │   ├── 📄 CallRouter.java           # Call routing logic
│   │   ├── 📄 CircularCallQueue.java    # Circular queue implementation
│   │   ├── 📄 PriorityCallQueue.java    # Priority queue implementation
│   │   └── 📄 StandardQueue.java        # Standard queue implementation
│   │
│   ├── 📁 model/                        # Data models
│   │   ├── 📄 Call.java                 # Call model class
│   │   └── 📄 CallStatus.java           # Call status enum/class
│   │
│   ├── 📁 experiment/                   # Experiment & testing classes
│   │   ├── 📄 Exp1_PriorityQueue.java   # Priority queue experiment
│   │   ├── 📄 Exp2_AgingAlgorithm.java  # Aging algorithm experiment
│   │   ├── 📄 Exp3_CallbackFairness.java   # Callback fairness experiment
│   │   └── 📄 RunAllExperiments.java    # Run all Experiments 
│   │
│   ├── 📁 storage/                      # Data storage & file handling
│   │   ├── 📄 CallHistoryStore.java     # Call history storage
│   │   ├── 📄 DataGenerator.java        # Data generation utility
│   │   └── 📄 FileHandler.java          # File handling operations
│   │
│   ├── 📁 ui/                           # User interface components
│   │   ├── 📄 ConsoleRenderer.java      # Console UI rendering
│   │   ├── 📄 InputHandler.java         # User input handling
│   │   └── 📄 MainMenu.java             # Main menu interface
│   │
│   └── 📁 main/                         # Main entry point
│       ├── 📄 Main.java                 # Main application class
│
├── 📁 data/                             # Data files directory
│   ├── 📄 call_history.csv              # Call history data
│   └── 📄 CustomerCalls.csv             # Customer calls data
│
├── 📁 docs/                             # Documentation directory
│   ├── 📁 AI_logs/                      # AI audit logs
│   │   ├── 📄 NguyenVanAn_AI_AuditLog.xlsx
│   │   └── 📄 NguyenVanAn_log.md
│   │
│   ├── 📁 diagrams/                     # UML and design diagrams
│   │   ├── 📄 class_diagram.drawio      # Class diagram
│   │   ├── 📄 .$class_diagram.drawio.bkp  # Class diagram backup
│   │   └── 📄 use_case_diagram.drawio   # Use case diagram
│   │
│   ├── 📁 diagrams description/         # Diagram descriptions
│   │   ├── 📄 class_diagram.docx        # Class diagram description docx
│   │   └── 📄 use_case_diagram.docx     # Use case diagram description docx
│   │
│   ├── 📁 others/                       # Other documentation
│   │
│   ├── 📄 diagrama                      # Mermaid flowchart code file
│   │
│   └── 📁 project_tree-folder/          # Project tree folder
│       └── 📄 tree-folder.md            # Tree folder markdown
│
└── 📁 out/                              # Output directory (build artifacts)
    └── [Compiled class files & artifacts]
