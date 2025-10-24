package com.ankitts.exception

/**
 * Exception thrown when an invalid row range is provided for Excel parsing.
 * This includes cases where start > end, start < 1, or end exceeds available rows.
 */
class InvalidRowRangeException(message: String) : Exception(message) {
    
    /**
     * Constructor that accepts both message and cause for exception chaining.
     */
    constructor(message: String, cause: Throwable) : super(message, cause)
}
