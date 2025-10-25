package com.ankitts.exception

/**
 * Custom exception class for TTS-specific errors.
 * Provides clear error context for TTS operations and allows the UI layer
 * to display appropriate error messages to users.
 */
class TTSException : Exception {
    
    /**
     * Constructor accepting a message parameter describing the TTS failure.
     * 
     * @param message Description of the TTS failure (e.g., initialization failed, voice not found, file write error)
     */
    constructor(message: String) : super(message)
    
    /**
     * Secondary constructor accepting both message and cause for exception chaining
     * when wrapping underlying Android TTS exceptions.
     * 
     * @param message Description of the TTS failure
     * @param cause The underlying exception that caused this TTS error
     */
    constructor(message: String, cause: Throwable) : super(message, cause)
}


