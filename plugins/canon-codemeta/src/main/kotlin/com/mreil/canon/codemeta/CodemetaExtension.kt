package com.mreil.canon.codemeta

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/** The `codemeta` contributor holder: configuration surface only. */
open class CodemetaExtension @Inject constructor(objects: ObjectFactory) {

    /** Whether this contributor participates. Most contributors carry this switch. */
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}
