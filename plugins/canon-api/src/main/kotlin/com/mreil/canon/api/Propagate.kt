package com.mreil.canon.api

/**
 * Marks a holder field for root-to-child propagation with the given semantics. Without it, only
 * `Property` fields propagate (as [SHALLOW][CopySemantics.SHALLOW]).
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Propagate(
    /** How the annotated field's value reaches child projects. */
    val value: CopySemantics = CopySemantics.SHALLOW,
)
