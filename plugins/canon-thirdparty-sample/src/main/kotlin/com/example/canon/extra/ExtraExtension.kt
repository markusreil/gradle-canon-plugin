package com.example.canon.extra

import com.mreil.canon.api.CopySemantics
import com.mreil.canon.api.Propagate
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/** The sample third-party `extra` holder: a single shallow string setting. */
open class ExtraExtension @Inject constructor(objects: ObjectFactory) {

    /** Third-party mode flag, propagated root → children as a shallow copy. */
    @Propagate(CopySemantics.SHALLOW)
    val mode: Property<String> = objects.property(String::class.java).convention("auto")
}
