# Resource Booking System (RBS) 🏫

## Overview 📝

The Resource Booking System (RBS) is a modern web application designed for Singapore Institute of Technology (SIT) students to conveniently book campus facilities for academic and recreational purposes. The system provides real-time facility availability checking, booking management, and automated notifications.

### Key Features 🌟
- Real-time facility availability checking
- Facility filtering and search
- Booking management system
- Credit-based booking system (4 hours/week per student)
- Email notifications for bookings and approvals
- Administrative facility management
- Role-based access control

### Tech Stack 💻
- Frontend: Angular
- Backend: Spring Boot
- Database: MySQL
- Containerization: Docker/Kubernetes
- Version Control: GitHub

## Getting Started 🚀

### Prerequisites
- Node.js (LTS version)
- Java JDK 17 or higher
- MySQL 8.0 or higher
- Bun (for development server)
- Maven
- Docker (optional)

### Frontend Setup ⚡

1. Install Bun
1.1 Windows Installation
```bash
powershell -c "irm bun.sh/install.ps1 | iex"
```
Restart shell and IDE

1.2 Linux / macOS Installation
```bash
curl -fsSL https://bun.sh/install | bash
```
Restart shell and IDE

2. Navigate to the frontend directory:
```bash
cd frontend
```

3. Install dependencies:

```bash
bun install
```

4. Start the development server:

```bash
bun run dev
```

The application will be available at http://localhost:4200

### Backend Setup 🛠️

1. Navigate to the backend directory:

```bash
cd backend
```

Run the application:

```bash
mvn spring-boot:run
```

The backend API will be available at http://localhost:8080


### Team Members 👥

- Nur Farah Nadiah Bte Kamsani
- Chen WuJie
- Lin Weichen
- Tong Yap Boon, Cyrus
- Walter Lay Zi Zheng