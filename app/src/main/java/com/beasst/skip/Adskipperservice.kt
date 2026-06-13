package com.beasst.skip

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * AdSkipperService
 *
 * An [AccessibilityService] that monitors window/content-change events and
 * automatically clicks any visible "Skip ad" button it finds, regardless of
 * whether the button itself is clickable (it will walk up the parent chain).
 *
 * Memory safety
 * ─────────────
 * Every [AccessibilityNodeInfo] reference that is obtained from the framework
 * (event source, child nodes, parent-chain nodes) is registered in a shared
 * recycle pool and released inside a `finally` block.  This prevents native
 * resource leaks on API < 33 where `recycle()` is not a no-op.
 *
 * Manifest requirements (already present in this project's AndroidManifest.xml):
 *   android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
 *   <intent-filter> with android.accessibilityservice.AccessibilityService
 *   <meta-data> pointing to res/xml/accessibility_service_config.xml
 */
class AdSkipperService : AccessibilityService() {

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG            = "AdSkipperService"
        private const val SKIP_AD_PHRASE = "skip ad"

        /**
         * Only act on event types that signal a UI update.
         * Filtering here avoids needless BFS work for irrelevant events
         * (e.g. TYPE_VIEW_SCROLLED, TYPE_VIEW_FOCUSED, etc.).
         */
        private val RELEVANT_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
    }

    // ── AccessibilityService overrides ────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Guard: null event or irrelevant type – bail immediately
        event ?: return
        if (event.eventType !in RELEVANT_EVENT_TYPES) return

        // ── Step 1: Verify the event source is not null ────────────────────
        // event.source returns the node that fired the event.
        // A null source means the event carries no inspectable UI subtree.
        val root: AccessibilityNodeInfo = event.source ?: return

        // ── Steps 2-5: BFS traversal + match + click + recycle ────────────
        // NOTE: rootInActiveWindow could be used here instead to guarantee a
        // full-window search, but event.source is preferred for efficiency —
        // it targets only the subtree that actually changed.
        findAndClickSkipAd(root)
    }

    override fun onInterrupt() {
        // Required by the framework; nothing to release here.
    }

    // ── Core BFS traversal ────────────────────────────────────────────────────

    /**
     * Performs a **breadth-first search** of the [AccessibilityNodeInfo] tree
     * rooted at [root], looking for the first node whose visible text or
     * content-description contains "skip ad" (case-insensitive).
     *
     * BFS is preferred over DFS here because "Skip ad" overlays tend to appear
     * near the top of the view hierarchy (shallow nodes), so BFS finds them
     * faster on average.
     *
     * Design decisions
     * ────────────────
     * • Every node reference — root, children, parent-chain — is added to
     *   [toRecycle] the moment we obtain it, *before* it is used.
     * • A `finally` block calls `recycle()` on all of them, so even an early
     *   `return true` inside the loop leaves no dangling references.
     * • `recycle()` is wrapped in try/catch to silently skip any node that was
     *   already freed (e.g. a node appearing twice in the pool due to a
     *   coincident parent lookup).
     * • On API 33+ `recycle()` is a documented no-op, so this is safe across
     *   the project's full minSdk 24 → targetSdk 36 range.
     *
     * @return `true` if a click was successfully dispatched, `false` otherwise.
     */
    @Suppress("DEPRECATION") // recycle() is no-op on API 33+ but required for API < 33
    private fun findAndClickSkipAd(root: AccessibilityNodeInfo): Boolean {

        // ── Step 5 (setup): pool tracks EVERY reference we ever hold ────────
        val toRecycle = mutableListOf<AccessibilityNodeInfo>()
        toRecycle.add(root)   // root is the first owned reference

        try {
            // ── Step 2: BFS with ArrayDeque (O(1) enqueue + dequeue) ────────
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()

                // ── Step 3: Case-insensitive match ───────────────────────────
                val text = current.text?.toString() ?: ""
                val desc = current.contentDescription?.toString() ?: ""

                val isMatch = text.contains(SKIP_AD_PHRASE, ignoreCase = true)
                        || desc.contains(SKIP_AD_PHRASE, ignoreCase = true)

                if (isMatch) {
                    // ── Step 4: Click direct node or the nearest clickable ──
                    // ancestor; all parent references are added to toRecycle
                    // so the finally block cleans them up too.
                    return clickNodeOrAncestor(current, toRecycle)
                }

                // Enqueue children.
                // getChild() allocates a new AccessibilityNodeInfo each time,
                // so we MUST register it in toRecycle before doing anything
                // else with it (the finally block is our only cleanup path).
                for (i in 0 until current.childCount) {
                    val child = current.getChild(i) ?: continue
                    toRecycle.add(child)   // ← register before enqueueing
                    queue.add(child)
                }
            }

        } finally {
            // ── Step 5 (teardown): recycle ALL nodes we ever referenced ─────
            // This runs whether we returned early (skip-ad found) or the while
            // loop exhausted the tree.  Nodes still sitting in `queue` at an
            // early return are already in toRecycle from when getChild() added
            // them — so they are released here even though dequeue never saw them.
            toRecycle.forEach { node ->
                try {
                    node.recycle()
                } catch (_: Exception) {
                    // IllegalStateException: already recycled — safe to ignore
                }
            }
        }

        return false
    }

    // ── Click helper ──────────────────────────────────────────────────────────

    /**
     * Attempts to click [node] if it is clickable; otherwise climbs the parent
     * chain until the nearest clickable ancestor is found and clicks that.
     *
     * Each call to `parent` allocates a fresh [AccessibilityNodeInfo], so every
     * reference obtained here is appended to [recyclePool] so the caller's
     * `finally` block recycles them along with the BFS nodes.
     *
     * @param node        The matched "skip ad" node.
     * @param recyclePool The shared pool managed by [findAndClickSkipAd].
     * @return `true` if a click was dispatched, `false` if no clickable node
     *         exists anywhere in the ancestor chain.
     */
    private fun clickNodeOrAncestor(
        node: AccessibilityNodeInfo,
        recyclePool: MutableList<AccessibilityNodeInfo>
    ): Boolean {

        // ── Direct click ─────────────────────────────────────────────────────
        if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "Clicked skip-ad node directly.")
            return true
        }

        // ── Walk up the parent chain ─────────────────────────────────────────
        // getParent() allocates a new object on every call, so each result must
        // be registered in recyclePool immediately after being obtained.
        var current: AccessibilityNodeInfo? = node.parent
        while (current != null) {
            recyclePool.add(current)   // register before any use

            if (current.isClickable) {
                current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Clicked clickable ancestor of skip-ad node.")
                return true
            }

            // Obtain the next level; current is already in recyclePool.
            current = current.parent
        }

        Log.w(TAG, "Skip-ad node found but no clickable node in ancestor chain.")
        return false
    }
}