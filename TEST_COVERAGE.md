# Test Coverage Summary

This document describes the comprehensive test suite created for the BlitzPay payment system, covering both payment functionality and server-side events (SSE).

## Test Files Created

### 1. PaymentRequestControllerTest.kt (5 tests)
Tests for the `/payments/request` endpoint that initiates payment requests.

**Test Coverage:**
- `createPaymentRequest should return accepted status with payment request id` - Verifies HTTP 202 response with payment request ID
- `createPaymentRequest should publish PaymentRequested event and emit to bus` - Validates event publishing and bus emission
- `createPaymentRequest should emit initial payment result to bus` - Tests initial payment result emission
- `createPaymentRequest should handle different currencies` - Tests GBP, EUR, USD currency support
- `createPaymentRequest should handle large amounts` - Validates handling of large payment amounts

**Key Functionality Tested:**
- POST /payments/request endpoint availability
- Payment request ID generation
- Event publishing mechanism
- Payment update bus integration
- Multi-currency support
- Large amount handling

### 2. QrPaymentSseControllerTest.kt (4 tests)
Tests for the Server-Sent Events (SSE) streaming endpoint `/qr-payments/{paymentRequestId}/events`.

**Test Coverage:**
- `stream endpoint should exist and accept GET requests` - Validates endpoint accessibility and content type
- `stream should handle valid UUID path parameter` - Tests UUID path parameter handling
- `stream should call paymentUpdateBus with correct payment request id` - Verifies correct payment request ID usage
- `stream should work with different payment request ids` - Tests independence of multiple SSE streams

**Key Functionality Tested:**
- GET /qr-payments/{id}/events endpoint availability
- SSE content type (text/event-stream)
- Payment request ID parameter handling
- PaymentUpdateBus integration
- Multiple concurrent streams

### 3. PaymentUpdateBusTest.kt (9 tests)
Unit tests for the PaymentUpdateBus component that manages reactive streams for payment updates.

**Test Coverage:**
- `sink should create new sink for payment request id` - Tests sink creation
- `sink should return same sink for same payment request id` - Validates sink caching
- `sink should return different sinks for different payment request ids` - Tests isolation between payment requests
- `emit should send payment result to sink` - Verifies event emission
- `emit should handle multiple payment results` - Tests multiple event emissions
- `complete should remove sink and complete flux` - Validates completion handling
- `complete should handle non-existent payment request id gracefully` - Tests error handling
- `emit should work for multiple concurrent payment requests` - Tests concurrency
- `new sink should be created after complete` - Validates sink recreation after completion

**Key Functionality Tested:**
- Reactive sink creation and management
- Event emission to sinks
- Sink completion handling
- Concurrent payment request handling
- Sink isolation and caching
- Error handling for missing sinks

### 4. PaymentInitRequestListenerTest.kt (8 tests)
Tests for the event listener that handles payment results and webhook notifications.

**Test Coverage:**
- `on PaymentResult should emit to payment update bus` - Tests PaymentResult event handling
- `on PaymentResult should handle multiple results` - Validates handling of multiple payment results
- `on TlWebhookEnvelope should complete payment when metadata contains paymentRequestId` - Tests webhook completion
- `on TlWebhookEnvelope should handle webhook without metadata gracefully` - Tests null metadata handling
- `on TlWebhookEnvelope should handle webhook with empty metadata gracefully` - Tests empty metadata handling
- `on TlWebhookEnvelope should handle webhook with invalid paymentRequestId` - Tests invalid UUID handling
- `on TlWebhookEnvelope should handle multiple webhooks` - Tests multiple webhook processing
- `on TlWebhookEnvelope should handle different event types` - Tests various webhook event types

**Key Functionality Tested:**
- PaymentResult event listening and emission
- Webhook event processing
- Payment completion via webhooks
- Metadata extraction and validation
- Error handling for invalid data
- Multiple event type support (payment_executed, payment_settled, payment_failed)

### 5. PaymentGatewayPaymentRequestedTest.kt (1 test - fixed)
Fixed existing test for TrueLayer payment integration.

**Test Coverage:**
- `listener calls gateway with mapped StartPaymentCommand` - Validates event-to-service integration

**What Was Fixed:**
- Updated mock return type from String to PaymentResult
- Added paymentRequestId parameter to PaymentRequested constructor

## Test Statistics

- **Total New Tests:** 26 tests across 4 new test files
- **Existing Tests Fixed:** 1 test
- **Test Success Rate:** 100% for new tests
- **Code Coverage Areas:**
  - Payment request handling
  - Server-Sent Events (SSE) streaming
  - Reactive event bus management
  - Event listener processing
  - Webhook handling

## Technologies Used

- **Testing Framework:** JUnit 5
- **Mocking:** Mockito with Kotlin support (mockito-kotlin)
- **Spring Testing:** @WebMvcTest, @WebFluxTest
- **Reactive Testing:** Reactor Test (StepVerifier)
- **Assertions:** AssertJ

## Running the Tests

```bash
# Run all new tests
./gradlew test --tests "*PaymentRequestControllerTest" \
                --tests "*QrPaymentSseControllerTest" \
                --tests "*PaymentUpdateBusTest" \
                --tests "*PaymentInitRequestListenerTest"

# Run specific test class
./gradlew test --tests "*PaymentRequestControllerTest"

# Run all tests
./gradlew test
```

## Key Features Validated

1. **Payment Flow:**
   - Payment request creation
   - Event publishing
   - Payment result emission
   - Multi-currency support

2. **Server-Side Events:**
   - SSE endpoint availability
   - Real-time payment updates streaming
   - Multiple concurrent streams
   - Proper content-type headers

3. **Event Bus:**
   - Reactive sink management
   - Event emission and consumption
   - Concurrent request handling
   - Stream completion

4. **Event Processing:**
   - Payment result propagation
   - Webhook processing
   - Payment completion
   - Error handling

## Notes

- Integration tests requiring database (PostgreSQL) were excluded as they require TestContainers setup
- WebhookController tests were excluded due to complex context loading and signature verification mocking requirements
- All tests follow existing repository conventions and patterns
- Tests are independent and can run in parallel
