# ATM Controller

A simple ATM controller implementation with Spring Boot and Kotlin that simulates basic banking operations without real hardware or bank systems.

## Features

- **Card Insertion & PIN Verification**: Secure authentication with 3-attempt limit
- **Account Selection**: Support for multiple accounts per card (Checking/Savings)
- **Balance Inquiry**: Check current account balance
- **Cash Deposit**: Add funds to selected account
- **Cash Withdrawal**: Withdraw funds with balance validation
- **Session Management**: Automatic session handling with security features
- **Transaction History**: All operations are logged and tracked

## Tech Stack

- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.5
- **Database**: H2 (in-memory)
- **Build Tool**: Gradle 8.14.3
- **Testing**: JUnit 5, Mockito

## Prerequisites

- JDK 17 or higher
- Gradle 8.x (or use included Gradle wrapper)

## Project Structure

```
src/
├── main/kotlin/com/bear/implementasimpleatmcontroller/
│   ├── controller/         # REST API endpoints
│   ├── domain/             # JPA entities
│   ├── repository/         # Data access layer
│   ├── service/
│   │   ├── atm/           # ATM business logic
│   │   └── bank/          # Mock bank service
│   └── config/            # Configuration & initial data
└── test/                  # Unit & integration tests
```

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd implement-a-simple-atm-controller
```

### 2. Build the Project

```bash
# Clean and build
./gradlew clean build

# Build without tests
./gradlew build -x test
```

### 3. Run Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "*CompleteATMControllerTest"

# Run with test report
./gradlew test --info
```

Test report will be generated at: `build/reports/tests/test/index.html`

### 4. Run the Application

```bash
# Start the application
./gradlew bootRun
```

The server will start at: `http://localhost:8080`

## API Documentation

### Endpoint

**URL**: `POST http://localhost:8080/atm/transaction`

### Request Format

```json
{
  "cardNumber": "string",
  "pin": "string",
  "accountNumber": "string",
  "transactionType": "CHECK_BALANCE | DEPOSIT | WITHDRAW",
  "amount": number (optional, required for DEPOSIT/WITHDRAW)
}
```

### Response Format

```json
{
  "success": boolean,
  "message": "string",
  "transactionType": "string",
  "accountNumber": "string",
  "accountType": "CHECKING | SAVINGS",
  "balance": number,
  "previousBalance": number,
  "newBalance": number,
  "transactionAmount": number,
  "errorCode": "string",
  "errorDetails": "string"
}
```

## Test Data

The application initializes with the following test data:

### Test Cards

| Card Number | PIN | Owner | Status |
|------------|-----|-------|--------|
| 1234567890123456 | 1234 | John Doe | Active |
| 9876543210987654 | 5678 | Jane Smith | Active |

### Test Accounts

| Card | Account Number | Type | Initial Balance |
|------|---------------|------|-----------------|
| Card 1 | ACC001 | CHECKING | $1,000 |
| Card 1 | ACC002 | SAVINGS | $5,000 |
| Card 2 | ACC003 | CHECKING | $2,000 |
| Card 2 | ACC004 | SAVINGS | $10,000 |

## API Test Examples

### 1. Check Balance

```bash
curl -X POST http://localhost:8080/atm/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "1234567890123456",
    "pin": "1234",
    "accountNumber": "ACC001",
    "transactionType": "CHECK_BALANCE"
  }'
```

### 2. Deposit Money

```bash
curl -X POST http://localhost:8080/atm/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "1234567890123456",
    "pin": "1234",
    "accountNumber": "ACC001",
    "transactionType": "DEPOSIT",
    "amount": 500
  }'
```

### 3. Withdraw Money

```bash
curl -X POST http://localhost:8080/atm/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "1234567890123456",
    "pin": "1234",
    "accountNumber": "ACC002",
    "transactionType": "WITHDRAW",
    "amount": 1000
  }'
```

## Testing with Postman

1. Create a new POST request
2. Set URL: `http://localhost:8080/atm/transaction`
3. Add header: `Content-Type: application/json`
4. Set body to raw JSON format
5. Use any of the example requests above

## H2 Database Console

Access the H2 console to view and query the database:

1. Navigate to: `http://localhost:8080/h2-console`
2. Connection details:
   - JDBC URL: `jdbc:h2:mem:atmdb`
   - User Name: `sa`
   - Password: (leave empty)
3. Click Connect

### Useful Queries

```sql
-- View all cards
SELECT * FROM CARDS;

-- View all accounts
SELECT * FROM ACCOUNTS;

-- View all transactions
SELECT * FROM TRANSACTIONS;

-- Check active sessions
SELECT * FROM ATM_SESSIONS WHERE SESSION_STATUS != 'CLOSED';
```

## Error Scenarios

The system handles various error cases:

- **Invalid PIN**: Returns error with remaining attempts
- **Card Blocked**: After 3 failed PIN attempts
- **Insufficient Balance**: When withdrawal amount exceeds balance
- **Account Not Found**: When specified account doesn't exist
- **Invalid Amount**: For negative or zero amounts

## Development

### Running in Development Mode

```bash
# Run with hot reload
./gradlew bootRun --continuous
```

### Code Quality

```bash
# Run linting (if configured)
./gradlew ktlintCheck

# Format code (if configured)
./gradlew ktlintFormat
```

## Test Coverage

The project includes comprehensive tests:

- **Unit Tests**: Service layer business logic
- **Integration Tests**: Full transaction flows
- **Controller Tests**: REST API endpoints
- **Total**: 79+ test cases with 100% pass rate

## Architecture Decisions

1. **Single API Endpoint**: All ATM operations through one transaction endpoint to simulate real ATM flow
2. **Session Management**: Maintains state between card insertion and transaction completion
3. **In-Memory Database**: H2 for simplicity and easy testing
4. **Mock Bank Service**: Simulates bank operations without external dependencies
5. **Domain-Driven Design**: Clear separation of concerns with domain models

## Security Considerations

- PIN attempts limited to 3 tries
- Card blocking after maximum failed attempts
- Session cleanup on errors
- No real sensitive data storage
- All transactions logged for audit

### Application won't start
- Check Java version: `java -version` (should be 17+)
- Check port 8080 is not in use
- Clear gradle cache: `./gradlew clean`

### Tests failing
- Run `./gradlew clean test`
- Check test reports in `build/reports/tests/test/index.html`

### Database issues
- H2 is in-memory, data resets on restart
- Check H2 console for current data state
