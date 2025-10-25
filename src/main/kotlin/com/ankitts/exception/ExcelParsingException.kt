package com.ankitts.exception

/**
 * Exception thrown when Excel file parsing fails due to corruption, invalid format,
 * or other issues that prevent Apache POI from reading the file.
 */
class ExcelParsingException(message: String) : Exception(message) {
    
    /**
     * Constructor that accepts both message and cause for exception chaining.
     * Used for wrapping underlying POI exceptions.
     */
    constructor(message: String, cause: Throwable) : super(message, cause)
}
