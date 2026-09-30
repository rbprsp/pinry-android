package dev.relony.pinry.ui.common

/** "#tag" for display. Some servers' tag names already start with '#'; those don't get a second one. */
fun tagLabel(tag: String): String = if (tag.startsWith("#")) tag else "#$tag"
