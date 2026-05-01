# Checkmate++

## How to run the application

### Start containers
```bash
docker run -d --name redis -p 6379:6379 redis
docker run -d --name postgres -e POSTGRES_USER=checkmate -e POSTGRES_PASSWORD=checkmate -e POSTGRES_DB=checkmate -p 5432:5432 postgres
```

### Frontend
- Navigate to the `frontend` directory:
  ```bash
  cd frontend
  ```
- Install dependencies:
  ```bash
  npm install
  ```
- Start the development server:
  ```bash
  npm run dev
  ```

### Backend
- Navigate to the `backend` directory:
  ```bash
  cd backend
  ```
- Run gradle "clean" task (fixes edge case of some gradle builds not running on rerun):
  ```bash
  ./gradlew clean
  ```
- Run gradle "backend" task:
  ```bash
  ./gradlew bootRun
  ```