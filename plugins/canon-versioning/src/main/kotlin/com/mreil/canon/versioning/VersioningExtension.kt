package com.mreil.canon.versioning

import com.mreil.canon.api.CopySemantics
import com.mreil.canon.api.Propagate
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/**
 * The `versioning` contributor holder: versioning scheme state. Parsing lives in the shared semver
 * service.
 */
open class VersioningExtension @Inject constructor(objects: ObjectFactory) {

    /** Active versioning scheme, propagated root → children as immutable. */
    @Propagate(CopySemantics.IMMUTABLE)
    val scheme: Property<String> = objects.property(String::class.java).convention("semver")
}
