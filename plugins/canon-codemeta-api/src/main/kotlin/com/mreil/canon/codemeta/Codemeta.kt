package com.mreil.canon.codemeta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A person or organization in a codemeta role. Object form only for now; plain-string forms some
 * tools accept are not supported yet.
 */
@Serializable
data class CodemetaPerson(
    /** Display name. Needed: POM developers, jReleaser authors. */
    val name: String,
    /** Contact address. Needed: POM developers. */
    val email: String? = null,
    /** Employer or group. Not needed: nothing consumes it yet. */
    val affiliation: String? = null,
    /** Given name. Not needed: display name suffices. */
    val givenName: String? = null,
    /** Family name. Not needed: display name suffices. */
    val familyName: String? = null,
)

/**
 * The `codemeta.json` file model: the complete CodeMeta v2/v3 term set. Each property is marked
 * needed (a consumer exists in this repo's arc) or not needed (kept for spec completeness so we can
 * review the list together and promote fields deliberately).
 */
@Serializable
data class Codemeta(
    /** JSON-LD context. Needed: identifies the vocabulary. */
    @SerialName("@context") val context: String = "https://doi.org/10.5063/schema/codemeta-2.0",
    /** JSON-LD type. Needed: always `SoftwareSourceCode` here. */
    @SerialName("@type") val type: String = "SoftwareSourceCode",

    /** The name of the software. Needed: POM name, jReleaser project name. */
    val name: String,
    /** What the software does. Needed: POM description, jReleaser announcements. */
    val description: String,
    /**
     * Version of the described software. Needed for file completeness; the build version stays
     * Gradle-owned.
     */
    val version: String? = null,
    /** Authors. Needed: POM developers, jReleaser authors. */
    val author: List<CodemetaPerson> = emptyList(),
    /** License, typically an SPDX id or URL. Needed: POM licenses, jReleaser license. */
    val license: String? = null,
    /** Where the readable source lives. Needed: POM scm, jReleaser repo links. */
    val codeRepository: String? = null,
    /** Tags describing the content. Needed: POM-adjacent discovery, keyword routing later. */
    val keywords: List<String> = emptyList(),
    /** Languages the software is written in. Needed: documents the Kotlin/Gradle stack. */
    val programmingLanguage: List<String> = emptyList(),
    /** Canonical URL of the item. Needed: POM url, jReleaser website. */
    val url: String? = null,

    /** Copyright holder. Needed: POM organization once we publish under one. */
    val copyrightHolder: List<CodemetaPerson> = emptyList(),
    /** Inception year. Needed: jReleaser project block. */
    val inceptionYear: String? = null,
    /**
     * Copyright notice. Maybe: announcements could use it; neither POM nor jReleaser require it.
     */
    val copyright: String? = null,
    /** Vendor. Not needed: no consumer; free-form extra at best. */
    val vendor: String? = null,
    /** Long description. Needed: jReleaser announcements fall back to it. */
    val longDescription: String? = null,
    /** Named links (homepage, documentation, bugTracker, ...). Needed: jReleaser links. */
    val links: Map<String, String>? = null,
    /** First publication date. Needed: changelog automation will key off dates. */
    val datePublished: String? = null,
    /**
     * Secondary contributors. Maybe: authors cover POM now; promote if a contributor needs credit.
     */
    val contributor: List<CodemetaPerson> = emptyList(),
    /** What changed in this version. Maybe: changelog automation is the natural consumer. */
    val releaseNotes: String? = null,
    /**
     * Runtime or interpreter dependencies (e.g. Java 17). Maybe: useful once contributors declare
     * toolchains.
     */
    val runtimePlatform: List<String> = emptyList(),
    /**
     * Maintainer (common extension, not a spec term). Maybe: POM developers may want a separate
     * contact.
     */
    val maintainer: List<CodemetaPerson> = emptyList(),

    /** Type of application (e.g. Game). Not needed: we build build-logic, not end-user apps. */
    val applicationCategory: String? = null,
    /** Subcategory of the application. Not needed: see applicationCategory. */
    val applicationSubCategory: String? = null,
    /** Citations of other works. Not needed: academic metadata, no consumer. */
    val citation: List<String> = emptyList(),
    /** Binary download URL. Not needed: artifacts come from Maven repos, not this file. */
    val downloadUrl: String? = null,
    /** The person who edited the work. Not needed: VCS owns edit history. */
    val editor: List<CodemetaPerson> = emptyList(),
    /** Media object encoding this work. Not needed: no embedded media. */
    val encoding: String? = null,
    /** MIME type of the content. Not needed: source is plain text. */
    val fileFormat: String? = null,
    /** Package size (e.g. 18MB). Not needed: build outputs report their own size. */
    val fileSize: String? = null,
    /** Financial sponsors. Not needed: no funding metadata yet. */
    val funder: List<CodemetaPerson> = emptyList(),
    /** Sub-works of this work. Not needed: single-repo scope. */
    val hasPart: List<String> = emptyList(),
    /** Formal identifiers (ISBN-like). Not needed: coordinates identify artifacts. */
    val identifier: String? = null,
    /** Install URL if different from the item URL. Not needed: Gradle resolves everything. */
    val installUrl: String? = null,
    /** Free-access flag. Not needed: all our output is open. */
    val isAccessibleForFree: Boolean? = null,
    /** Parent work this is part of. Not needed: single-repo scope. */
    val isPartOf: String? = null,
    /** Minimum memory requirements. Not needed: build-logic has no runtime footprint. */
    val memoryRequirements: String? = null,
    /** Required OS (e.g. Windows 7). Not needed: JVM abstracts this away. */
    val operatingSystem: List<String> = emptyList(),
    /** Runtime permissions (mobile-style). Not needed: irrelevant on the JVM. */
    val permissions: String? = null,
    /** Position in a series. Not needed: JSON arrays already order authors. */
    val position: String? = null,
    /** Processor architecture required. Not needed: JVM abstracts this away. */
    val processorRequirements: String? = null,
    /** Producer (album/movie-style credit). Not needed: no such role here. */
    val producer: List<CodemetaPerson> = emptyList(),
    /** Service provider. Not needed: no hosted service. */
    val provider: List<CodemetaPerson> = emptyList(),
    /** Publisher of the work. Not needed: self-published via Maven repos. */
    val publisher: List<CodemetaPerson> = emptyList(),
    /** Related links. Not needed: url plus repo links suffice. */
    val relatedLink: List<String> = emptyList(),
    /** Code reviews. Not needed: review lives in the forge. */
    val review: List<String> = emptyList(),
    /** Reference page (Wikipedia, Wikidata). Not needed: no such page. */
    val sameAs: String? = null,
    /** Application help. Not needed: build-logic has user docs elsewhere. */
    val softwareHelp: String? = null,
    /** Required software dependencies. Not needed: Gradle owns the dependency graph. */
    val softwareRequirements: List<String> = emptyList(),
    /** Suggested software dependencies. Not needed: see softwareRequirements. */
    val softwareSuggestions: List<String> = emptyList(),
    /** Corporate sponsor. Not needed: see funder. */
    val sponsor: List<CodemetaPerson> = emptyList(),
    /** Free-space requirements. Not needed: see memoryRequirements. */
    val storageRequirements: String? = null,
    /** Supporting data feeds. Not needed: no datasets. */
    val supportingData: List<String> = emptyList(),
    /** Target OS/product the code applies to. Not needed: see operatingSystem. */
    val targetProduct: String? = null,
    /** Creation date. Not needed: VCS owns history. */
    val dateCreated: String? = null,
    /** Last-modification date. Not needed: VCS owns history. */
    val dateModified: String? = null,
    /** Copyright year. Not needed: license files carry this. */
    val copyrightYear: String? = null,
    /**
     * Version as embodied by a resource (alias-ish twin of version). Not needed: one version field
     * is enough.
     */
    val softwareVersion: String? = null,
)
