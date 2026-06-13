project-root/
├── src/
│   ├── Main/
│   │   └── Main.java                 # Lớp khởi chạy ứng dụng chính
│   │
│   ├── model/                        # Quản lý các đối tượng dữ liệu
│   │   ├── Call.java                 # Thuộc tính cuộc gọi (Mã KH, Số ĐT, Loại KH, Số lần gọi lại, Điểm ưu tiên)
│   │   └── CallStatus.java           # Enum các trạng thái cuộc gọi (WAITING, PROCESSING, COMPLETED, MISSED)
│   │
│   ├── core/                         # Chứa thuật toán và xử lý nghiệp vụ hàng đợi
│   │   ├── StandardQueue.java        # Hàng đợi FIFO thông thường (để so sánh hiệu năng)
│   │   ├── PriorityCallQueue.java    # Hàng đợi ưu tiên chính - Tự động sắp xếp theo Priority Score khi chèn
│   │   ├── CircularCallQueue.java    # Hàng đợi vòng - Giới hạn tối đa cuộc gọi chờ trong hệ thống
│   │   ├── CallProcessor.java        # [MỚI] Bộ xử lý: Đọc CSV thô, tính điểm ưu tiên và tự động nạp vào Queue
│   │   ├── CallRouter.java           # Bộ điều phối cuộc gọi từ hàng chờ đến điện thoại viên (Agent)
│   │   └── AgingAlgorithm.java       # Thuật toán tăng độ ưu tiên theo thời gian chờ (Chống trôi dữ liệu)
│   │
│   ├── ui/                           # Xử lý giao diện Console
│   │   ├── MainMenu.java             # Vòng lặp menu (Thêm chức năng: 1. Generate Data -> 2. Auto Sort & Load)
│   │   ├── InputHandler.java         # Nhận dữ liệu bàn phím và kiểm tra hợp lệ
│   │   └── ConsoleRenderer.java      # In giao diện, bảng biểu hiển thị danh sách 10,000 cuộc gọi sau khi sắp xếp
│   │
│   ├── storage/                      # Quản lý lưu trữ tệp tin
│   │   ├── FileHandler.java          # Đọc/ghi tệp tin cơ bản (thô)
│   │   ├── CallHistoryStore.java     # Quản lý đọc/ghi lịch sử cuộc gọi đã xử lý thành công
│   │   └── DataGenerator.java        # [MỚI] Tự động sinh 10,000 khách hàng ngẫu nhiên (VIP, Thường, Gọi nhiều...) ra CSV
│   │
│   └── experiment/                   # Các bài thực nghiệm / Stress test thuật toán
│       ├── Exp1_PriorityQueue.java   # Đo thời gian sắp xếp và xử lý 10,000 cuộc gọi
│       ├── Exp2_AgingAlgorithm.java  # Kiểm tra thuật toán tăng điểm ưu tiên với số lượng data lớn
│       └── Exp3_CallbackFairness.java   # Thử nghiệm tính công bằng của Callback
│
├── data/                             # Chứa các dữ liệu lưu trữ
│   ├── call_history.csv              # Tệp tin lưu lịch sử các cuộc gọi đã phục vụ xong
│   └── CustomerCalls.csv             # [MỚI] Tệp dữ liệu 10,000 cuộc gọi thô, ngẫu nhiên vừa được sinh ra (chưa xếp lịch)
│
├── docs/                             # Tài liệu dự án
│   ├── AI_logs/
│   │   ├── NguyenVanAn_AI_AuditLog.xlsx  # Bảng Excel Audit Log
│   │   └── NguyenVanAn_log.md        # Nhật ký hội thoại AI
│   │
│   ├── diagrams/
│   │   ├── use_case_diagram.drawio  # Sơ đồ use case Draw.io
│   │   └── class_diagram.drawio     # Sơ đồ lớp Draw.io
│   │
│   ├── diagrams description/
│   │   ├── class_diagram.docx       # Mô tả sơ đồ lớp
│   │   └── use_case_diagram.docx    # Mô tả sơ đồ use case
│   │
│   └── diagrama                      # Tệp mã nguồn vẽ Mermaid Flowchart của project
│
└── README.md