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
│   │   └── 📄 RunAllExperiments.java    # Experiment runner
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
│   │
│   ├── 📁 others/                       # Other documentation
│   │
│   └── 📁 project_tree-folder/          # Project tree folder
│       └── 📄 tree-folder.md            # Tree folder markdown
│
└── 📁 out/                              # Output directory (build artifacts)
    └── [Compiled class files & artifacts]
```

---

## 📊 Project Structure Summary

### Key Directories:

| Directory | Purpose | Contains |
|-----------|---------|----------|
| `src/` | Source code | All Java source files organized by module |
| `src/config/` | Configuration management | Settings loader and properties |
| `src/core/` | Core algorithms | Queue implementations, routing, processing |
| `src/model/` | Data models | Call and CallStatus classes |
| `src/experiment/` | Testing & experiments | Experiment implementations |
| `src/storage/` | Data persistence | File handling and storage logic |
| `src/ui/` | User interface | Console rendering and menu system |
| `data/` | Data files | CSV files with call history data |
| `docs/` | Documentation | Diagrams, logs, and project information |
| `out/` | Build output | Compiled class files |

---

## 📝 File Statistics

### Java Source Files:
- **Total Java files**: 19
- **Core modules**: 6 files (queue, routing, processing)
- **Model classes**: 2 files
- **Experiment files**: 4 files
- **Storage utilities**: 3 files
- **UI components**: 3 files
- **Config**: 1 file

### Data Files:
- **CSV files**: 2 (call_history.csv, CustomerCalls.csv)

### Documentation:
- **Diagrams**: 2 (class & use case diagrams)
- **Logs**: 2 (markdown & Excel)

---

## 🎯 Module Breakdown

### Core Business Logic Modules:
1. **Queue Management**: `CircularCallQueue.java`, `PriorityCallQueue.java`, `StandardQueue.java`
2. **Call Processing**: `CallProcessor.java`, `CallRouter.java`
3. **Algorithms**: `AgingAlgorithm.java`
4. **Configuration**: `ConfigLoader.java`, `settings.properties`

### Data Layer:
- `Call.java` - Call entity model
- `CallStatus.java` - Status tracking
- `CallHistoryStore.java` - Storage operations
- `FileHandler.java` - File I/O
- `DataGenerator.java` - Data generation

### Presentation Layer:
- `MainMenu.java` - Menu navigation
- `ConsoleRenderer.java` - Output rendering
- `InputHandler.java` - Input processing

### Entry Point:
- `Main.java` - Application bootstrap

---



