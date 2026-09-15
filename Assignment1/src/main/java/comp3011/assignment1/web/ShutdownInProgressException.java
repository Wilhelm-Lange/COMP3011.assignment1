package comp3011.assignment1.web;

// thrown when a shutdown is asked for but one is already running -> 409
public class ShutdownInProgressException extends RuntimeException {
}
