package com.mreil.canon.api

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Propagate(val value: CopySemantics = CopySemantics.SHALLOW)
