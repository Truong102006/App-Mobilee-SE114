# Planning: Implement Backend Features from rw.md

## Overview
We will implement 5 feature areas:
1. Chat enhancements (emoji reactions, replies, message editing).
2. User info chat page (detailed profile viewing, media attachments listing, blocking users).
3. Privacy & security (password resets, delete account).
4. Authorization (strict verification that only post owners can update/delete community posts).
5. Edit profile (basic details, avatar from albums, social links).

## Project Type
BACKEND (Spring Boot API with Firestore)

## Success Criteria
- All 11 new classes compile and function correctly.
- All 7 modified classes compile and function correctly.
- All MockMvc integration tests run and pass using `mvnw test`.

## Tech Stack
- Java 17 / Spring Boot 3
- Google Cloud Firestore & Firebase Admin SDK
- JUnit 5 & MockMvc (Testing)

## File Structure
We will add/modify code in the following locations:
- `com.soulmate.backend.dto.chat`
- `com.soulmate.backend.dto.user`
- `com.soulmate.backend.dto.privacy`
- `com.soulmate.backend.controller`
- `com.soulmate.backend.service`
- `com.soulmate.backend.controller` (Tests)

## Task Breakdown

### Phase 1: DTO Setup
- **Task D1**: Modify `ChatMessageItemResponse.java` and `SendChatMessageRequest.java` to support reply/react/edit.
- **Task D2**: Create `ReactMessageRequest.java`, `EditMessageRequest.java`, `UserProfileResponse.java`, `SocialLink.java`, `BlockUserResponse.java`, `UpdateProfileRequest.java`, `UpdateAvatarRequest.java`, `ChangePasswordRequest.java`.

### Phase 2: User Profile and Blocking Services
- **Task S1**: Update `UserService.java` to support fetching/updating profiles, blocking, and unblocking users.
- **Task S2**: Implement block controls checking in `ChatService.java#sendMessage()`.

### Phase 3: Chat Interaction Services
- **Task S3**: Update `ChatService.java` to handle reply processing in `sendMessage`, reacts in `reactToMessage`, message editing in `editMessage`, and fetching media list in `listConversationMedia`.

### Phase 4: Privacy & Account Services
- **Task S4**: Create `PrivacyService.java` to handle Firebase Password Reset email sending and user account deletion from Firestore and Firebase Authentication.

### Phase 5: Controllers Implementation
- **Task C1**: Implement `UserController.java` (profile read/write, blocks).
- **Task C2**: Implement `PrivacyController.java` (password resets, delete account).
- **Task C3**: Modify `ChatController.java` to expose reactions, editing, and media endpoints.
- **Task C4**: Modify `CommunityController.java` and `CommunityService.java` to expose author profile endpoint.

### Phase 6: Integration Testing
- **Task T1**: Create `ChatControllerIntegrationTest.java`.
- **Task T2**: Create `UserControllerIntegrationTest.java`.
- **Task T3**: Create `PrivacyControllerIntegrationTest.java`.
- **Task T4**: Modify `CommunityControllerIntegrationTest.java` to assert Forbidden rules on unauthorized post deletes/updates.

## Phase X: Verification
- [ ] Run `./mvnw clean test` for all suites
- [ ] Verification script audit
- [ ] Compliance manual check (Socratic Gate, no purple/template rules)
