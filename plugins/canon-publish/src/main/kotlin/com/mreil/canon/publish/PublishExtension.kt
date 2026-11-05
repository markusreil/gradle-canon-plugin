package com.mreil.canon.publish

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/**
 * The `publish` contributor holder: pure configuration surface, no internal state.
 */
open class PublishExtension @Inject constructor(objects: ObjectFactory) {

    /**
     * Whether this contributor participates. Most contributors carry this switch.
     */
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}
