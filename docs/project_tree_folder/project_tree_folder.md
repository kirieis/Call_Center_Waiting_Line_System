# Project Directory Tree (Optimized)

Đây là cây thư mục đã được tối ưu hóa vừa phải (Moderate Optimization) cho dự án **Call Center Waiting Line System**. Cấu trúc này giữ vững tính mô-đun và lập trình hướng đối tượng (OOP) nhưng làm phẳng các package con quá sâu và loại bỏ các thành phần dư thừa.

```text
project-root/
├── src/
│   └── callcenter/
│       ├── Main.java                # Lớp khởi chạy ứng dụng chính
│       │
│       ├── model/                   # Quản lý các đối tượng dữ liệu
│       │   ├── Call.java            # Thông tin và điểm ưu tiên cuộc gọi
│       │   └── CallStatus.java      # Enum các trạng thái cuộc gọi
│       │
│       ├── core/                    # Chứa thuật toán và xử lý nghiệp vụ hàng đợi
│       │   ├── StandardQueue.java   # Hàng đợi FIFO thông thường (để so sánh)
│       │   ├── PriorityCallQueue.java # Hàng đợi ưu tiên (Priority Queue) chính
│       │   ├── CallRouter.java      # Bộ điều phối cuộc gọi đến điện thoại viên
│       │   └── AgingAlgorithm.java  # Thuật toán tăng độ ưu tiên theo thời gian chờ (Chống trôi)
│       │
│       ├── ui/                      # Xử lý giao diện Console
│       │   ├── MainMenu.java        # Vòng lặp điều khiển menu chính
│       │   ├── InputHandler.java    # Nhận dữ liệu bàn phím và kiểm tra hợp lệ
│       │   └── ConsoleRenderer.java # In giao diện và hiển thị dữ liệu bảng biểu
│       │
│       ├── storage/                 # Quản lý lưu trữ tệp tin
│       │   ├── FileHandler.java     # Đọc/ghi tệp tin cơ bản (thô)
│       │   └── CallHistoryStore.java# Quản lý đọc/ghi lịch sử cuộc gọi định dạng CSV
│       │
│       └── experiment/              # Các bài thực nghiệm/đánh giá thuật toán
│           ├── Exp1_PriorityQueue.java
│           ├── Exp2_AgingAlgorithm.java
│           ├── Exp3_HistoryLookup.java
│           └── Exp4_ConsoleRender.java
│
├── test/                            # Thư mục chứa các kiểm thử
│   ├── AgingAlgorithmTest.java      # Kiểm thử thuật toán chống trôi
│   └── CallQueueTest.java           # Kiểm thử độ chính xác của Priority Queue
│
├── data/                            # Chứa các dữ liệu lưu trữ
│   └── call_history.csv             # Tệp tin lưu lịch sử cuộc gọi
│
├── docs/                            # Tài liệu dự án
│   ├── AI_logs/
│   └── diagrams/
│       ├── use_case_diagram.drawio  # Sơ đồ use case
│       └── class_diagram.drawio     # Sơ đồ lớp
│
└── README.md
```