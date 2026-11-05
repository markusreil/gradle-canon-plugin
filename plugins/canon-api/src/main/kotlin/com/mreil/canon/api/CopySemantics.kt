package com.mreil.canon.api

/** How a contributor-holder field value reaches child projects. */
enum class CopySemantics {
    /** Copy the value; children may override it. */
    SHALLOW,
    /** Merge the value with the child's; reserved until the deep-merge chunk. */
    DEEP,
    /** Copy the value and fail the build if a child changes it. */
    IMMUTABLE,
}
