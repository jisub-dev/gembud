package com.gembud.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error codes for application exceptions.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Authentication & Authorization
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH001", "Invalid email or password"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH002", "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH003", "Access denied"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH004", "Token has expired"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH005", "Invalid token"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH006", "Email already exists"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER001", "User not found"),
    LOW_TEMPERATURE(HttpStatus.FORBIDDEN, "USER002", "Temperature too low to create room"),

    // Game
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME001", "Game not found"),

    // Room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM001", "Room not found"),
    ROOM_FULL(HttpStatus.CONFLICT, "ROOM002", "Room is full"),
    ROOM_CLOSED(HttpStatus.CONFLICT, "ROOM003", "Room is closed"),
    ALREADY_IN_ROOM(HttpStatus.CONFLICT, "ROOM004", "Already in this room"),
    NOT_IN_ROOM(HttpStatus.BAD_REQUEST, "ROOM005", "Not in this room"),
    INVALID_ROOM_PASSWORD(HttpStatus.UNAUTHORIZED, "ROOM006", "Invalid room password"),
    NOT_HOST(HttpStatus.FORBIDDEN, "ROOM007", "Only host can perform this action"),

    // Evaluation
    ROOM_NOT_CLOSED_FOR_EVALUATION(HttpStatus.BAD_REQUEST, "EVAL001", "Can only evaluate after room is closed"),
    EVALUATOR_NOT_IN_ROOM(HttpStatus.BAD_REQUEST, "EVAL002", "Evaluator was not in this room"),
    EVALUATED_NOT_IN_ROOM(HttpStatus.BAD_REQUEST, "EVAL003", "Evaluated user was not in this room"),
    ALREADY_EVALUATED(HttpStatus.CONFLICT, "EVAL004", "Already evaluated this user for this room"),
    CANNOT_EVALUATE_SELF(HttpStatus.BAD_REQUEST, "EVAL005", "Cannot evaluate yourself"),

    // Chat
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT001", "Chat room not found"),
    NOT_CHAT_MEMBER(HttpStatus.FORBIDDEN, "CHAT002", "Not a member of this chat room"),
    CHAT_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "CHAT003", "Message cannot be empty"),

    // Validation
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "VAL001", "Invalid input"),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "VAL002", "Missing required field"),

    // Server
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SRV001", "Internal server error"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SRV002", "Service temporarily unavailable");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
